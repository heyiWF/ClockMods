package com.clockmods.calendar.imports;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import androidx.lifecycle.MutableLiveData;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Private source files plus an asynchronous month cache; UI reads never parse an ICS file. */
public final class UserCalendarStore {
    private static UserCalendarStore instance;
    public static synchronized UserCalendarStore get(Context context) {
        if(instance==null)instance=new UserCalendarStore(context.getApplicationContext());
        return instance;
    }
    public static final class Source {
        public final String id,name,title;public final int eventCount;public final long bytes;
        Source(String id,String name,String title,int count,long bytes){this.id=id;this.name=name;this.title=title;this.eventCount=count;this.bytes=bytes;}
    }
    public static final class Occurrence {
        public final Source source; public final ImportedEvent event;
        Occurrence(Source source,ImportedEvent event){this.source=source;this.event=event;}
        public String id(){return source.id+"/"+event.id;}
    }
    public final MutableLiveData<Long> changes=new MutableLiveData<>(0L);
    private final Context context;
    private final File directory;
    private final AtomicFile catalog;
    private List<Source> sources=Collections.emptyList();
    private final Map<String,Map<LocalDate,List<Occurrence>>> months=new LinkedHashMap<String,Map<LocalDate,List<Occurrence>>>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Map<LocalDate,List<Occurrence>>> entry){return size()>12;}
    };
    private final Map<String,String> errors=new HashMap<>();
    private final Set<String> pending=new HashSet<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private long generation,revision;
    private UserCalendarStore(Context context){
        this.context=context;directory=new File(context.getFilesDir(),"imported-calendars");
        catalog=new AtomicFile(new File(directory,"catalog.json"));
        try(InputStream input=catalog.openRead()){
            byte[] bytes=ICalendarParser.readBytes(input);
            JSONArray array=new JSONObject(new String(bytes,StandardCharsets.UTF_8)).getJSONArray("files");
            List<Source> loaded=new ArrayList<>();
            for(int i=0;i<Math.min(array.length(),32);i++){
                JSONObject item=array.getJSONObject(i);String id=item.getString("id");
                if (!id.matches("[a-f0-9]{64}")) continue;
                File original = file(id), trash = new File(directory, id + ".deleted");
                // A process death before the catalog commit must not hide a retained source.
                if (!original.isFile() && trash.isFile()) trash.renameTo(original);
                loaded.add(new Source(id, item.getString("name"), item.optString("title"),
                        item.getInt("count"), original.length()));
                if (!original.isFile()) errors.put(id, "invalid");
            }
            sources=Collections.unmodifiableList(loaded);
            File[] remnants = directory.listFiles((dir, filename) -> filename.matches("[a-f0-9]{64}\\.deleted"));
            if (remnants != null) for (File remnant : remnants) {
                String id = remnant.getName().substring(0, 64);
                if (loaded.stream().noneMatch(source -> source.id.equals(id))) remnant.delete();
            }
        }catch(Exception ignored){/* A missing/corrupt catalog never overwrites the retained source files. */}
    }
    public synchronized List<Source> files(){return sources;}
    public synchronized String error(String id){return errors.get(id);}
    private File file(String id){return new File(directory,id+".ics");}
    public Source importUri(Uri uri)throws IOException {
        String name="calendar.ics";
        try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);
        }
        try(InputStream input=context.getContentResolver().openInputStream(uri)){
            if(input==null)throw new CalendarImportException("invalid");return importStream(input,name);
        }
    }
    public Source importStream(InputStream input,String originalName)throws IOException {
        byte[] data=ICalendarParser.readBytes(input);String id=hash(data);
        synchronized(this){for(Source source:sources)if(source.id.equals(id))throw new CalendarImportException("duplicate");}
        ICalendarParser parsed=ICalendarParser.parse(data,TimeZone.getTimeZone("UTC"));
        LocalDate first=LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        parsed.between(first,first.plusMonths(1));
        String name=originalName==null?"calendar.ics":originalName.replace('\u0000',' ').trim();
        if(name.isEmpty())name="calendar.ics";name=name.substring(0,Math.min(name.length(),240));
        Source result=new Source(id,name,parsed.name.isEmpty()?name:parsed.name,parsed.eventCount,data.length);
        synchronized(this){
            long bytes=data.length;for(Source source:sources){bytes+=source.bytes;if(source.id.equals(id))throw new CalendarImportException("duplicate");}
            if(sources.size()>=32||bytes>32L*1024*1024)throw new CalendarImportException("limit");
            if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create calendar directory");
            File target=file(id),temporary=new File(directory,"pending-"+UUID.randomUUID());boolean created=!target.exists();
            try{
                if(created){try(FileOutputStream stream=new FileOutputStream(temporary)){stream.write(data);stream.getFD().sync();}
                    if(!temporary.renameTo(target))throw new IOException("Cannot retain calendar file");}
                List<Source> next=new ArrayList<>(sources);next.add(result);writeCatalog(next);sources=Collections.unmodifiableList(next);
            }catch(IOException failure){if(created)target.delete();throw failure;}finally{temporary.delete();}
            invalidate();
        }
        return result;
    }
    public synchronized boolean delete(String id)throws IOException {
        Source removed=null;List<Source> next=new ArrayList<>();
        for(Source source:sources){if(source.id.equals(id))removed=source;else next.add(source);}
        if(removed==null)return false;
        File original=file(id),trash=new File(directory,id+".deleted");
        if(original.exists() && !original.renameTo(trash))throw new IOException("Cannot remove calendar file");
        try{writeCatalog(next);}catch(IOException failure){trash.renameTo(original);throw failure;}
        if (trash.exists() && !trash.delete()) {
            // Keep the source manageable so the user can retry a failed physical deletion.
            writeCatalog(sources); trash.renameTo(original);
            throw new IOException("Cannot remove calendar file");
        }
        sources=Collections.unmodifiableList(next);invalidate();
        return true;
    }
    private void writeCatalog(List<Source> sources)throws IOException {
        FileOutputStream stream=null;
        try {
            JSONArray array=new JSONArray();for(Source source:sources)array.put(new JSONObject().put("id",source.id).put("name",source.name).put("title",source.title).put("count",source.eventCount));
            byte[] bytes=new JSONObject().put("version",1).put("files",array).toString().getBytes(StandardCharsets.UTF_8);
            stream=catalog.startWrite();stream.write(bytes);catalog.finishWrite(stream);
        }catch(Exception failure){if(stream!=null)catalog.failWrite(stream);throw new IOException("Cannot save calendar catalog",failure);}
    }
    private void invalidate(){generation++;months.clear();pending.clear();errors.clear();changes.postValue(++revision);}
    public synchronized List<Occurrence> eventsOn(int year,int month,int day,TimeZone timezone){
        LocalDate date=LocalDate.of(year,month+1,day),first=date.withDayOfMonth(1);
        String key=generation+"/"+first+"/"+timezone.getID();
        Map<LocalDate,List<Occurrence>> cached=months.get(key);
        if(cached!=null)return cached.getOrDefault(date,Collections.emptyList());
        if(sources.isEmpty())return Collections.emptyList();
        if(pending.size()<24 && pending.add(key)){
            List<Source> snapshot=sources;long version=generation;TimeZone zone=(TimeZone)timezone.clone();
            worker.execute(()->loadMonth(key,first,zone,snapshot,version));
        }
        return Collections.emptyList();
    }
    private void loadMonth(String key,LocalDate first,TimeZone zone,List<Source> snapshot,long version){
        Map<LocalDate,List<Occurrence>> result=new HashMap<>();Map<String,String> failures=new HashMap<>();int count=0;
        for(Source source:snapshot){
            try(InputStream input=new FileInputStream(file(source.id))){
                List<ImportedEvent> events=ICalendarParser.parse(ICalendarParser.readBytes(input),zone).between(first,first.plusMonths(1));
                if(count+events.size()>ICalendarParser.MAX_OCCURRENCES)throw new CalendarImportException("limit");count+=events.size();
                for(LocalDate date=first;date.isBefore(first.plusMonths(1));date=date.plusDays(1))for(ImportedEvent event:events)
                    if(event.occursOn(date,zone.toZoneId()))result.computeIfAbsent(date,ignored->new ArrayList<>()).add(new Occurrence(source,event));
            }catch(Exception failure){failures.put(source.id,failure instanceof CalendarImportException?((CalendarImportException)failure).reason:"invalid");}
        }
        for(Map.Entry<LocalDate,List<Occurrence>> entry:result.entrySet()){
            LocalDate date=entry.getKey();entry.getValue().sort(Comparator.comparingInt((Occurrence item)->item.event.minuteOfDay(date,zone.toZoneId())).thenComparing(item->item.event.title));
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        synchronized(this){
            pending.remove(key);if(version!=generation)return;
            // A successful adjacent month must not erase another month's expansion warning.
            months.put(key,Collections.unmodifiableMap(result));errors.putAll(failures);changes.postValue(++revision);
        }
    }
    private static String hash(byte[] data)throws IOException {
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder text=new StringBuilder();for(byte b:digest)text.append(String.format(Locale.ROOT,"%02x",b&255));return text.toString();}
        catch(Exception failure){throw new IOException(failure);}
    }
}
