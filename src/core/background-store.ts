/**
 * Background image storage.
 *
 * Replaces com.clockmods.background.BackgroundRepository's file handling: the
 * picked image is downsampled to the screen's long edge and kept in IndexedDB as
 * a Blob. EXIF orientation, which Android applied by hand through
 * ExifInterface + Matrix, is handled by `createImageBitmap`'s
 * `imageOrientation: 'from-image'`.
 */

const DB_NAME = 'clockmods';
const DB_VERSION = 1;
const STORE = 'background';
const KEY = 'image';

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE);
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function withStore<T>(
  mode: IDBTransactionMode,
  action: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T> {
  const db = await openDatabase();
  try {
    return await new Promise<T>((resolve, reject) => {
      const transaction = db.transaction(STORE, mode);
      const request = action(transaction.objectStore(STORE));
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  } finally {
    db.close();
  }
}

/**
 * Decodes, orients and downsamples `file`, then stores it.
 *
 * @param longEdge the screen's long edge in device pixels; the image is scaled so
 *   its own long edge matches, which is what BackgroundRepository.saveImage did
 *   with BitmapFactory's inSampleSize.
 */
export async function saveBackgroundImage(file: Blob, longEdge: number): Promise<void> {
  const source = await createImageBitmap(file, { imageOrientation: 'from-image' });
  try {
    const scale = Math.min(1, longEdge / Math.max(source.width, source.height));
    const width = Math.max(1, Math.round(source.width * scale));
    const height = Math.max(1, Math.round(source.height * scale));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) throw new Error('2D canvas unavailable');
    context.drawImage(source, 0, 0, width, height);
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', 0.92)
    );
    if (!blob) throw new Error('image encoding failed');
    await withStore('readwrite', (store) => store.put(blob, KEY));
  } finally {
    source.close();
  }
}

let objectUrl: string | null = null;

/**
 * @returns an object URL for the stored image, or null when none is stored. The
 *   previous URL is revoked on each call, so callers must not hold onto it.
 */
export async function backgroundImageUrl(): Promise<string | null> {
  let blob: Blob | undefined;
  try {
    blob = await withStore<Blob | undefined>('readonly', (store) => store.get(KEY));
  } catch {
    blob = undefined;
  }
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl);
    objectUrl = null;
  }
  if (!blob) return null;
  objectUrl = URL.createObjectURL(blob);
  return objectUrl;
}

export async function hasBackgroundImage(): Promise<boolean> {
  try {
    const blob = await withStore<Blob | undefined>('readonly', (store) => store.get(KEY));
    return blob !== undefined;
  } catch {
    return false;
  }
}

export async function clearBackgroundImage(): Promise<void> {
  try {
    await withStore('readwrite', (store) => store.delete(KEY));
  } catch {
    /* nothing stored */
  }
}
