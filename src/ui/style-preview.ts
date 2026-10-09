import { localizeUiTree } from '../core/ui-language';
import { prefs } from '../core/prefs';
import { readBackgroundBlob } from '../core/background-store';
import { ClockPage } from '../pages/clock';
import { CalendarPage } from '../pages/calendar';
import { ensureFontLoaded } from '../core/fonts';
import { element } from './controls';
import markup from '../../index.html?raw';
import tokens from '../styles/tokens.css?inline';
import fonts from '../styles/fonts.css?inline';
import material from '../styles/material.css?inline';
import base from '../styles/base.css?inline';
import clock from '../styles/clock.css?inline';
import calendar from '../styles/calendar.css?inline';

export interface PreviewDraft { settings: typeof prefs; image: File | null; }

/** An isolated viewport renders drafts without writes, weather requests or live-page mutations. */
export class StylePreview {
  readonly element = element('section', 'settings-live-preview');
  private readonly viewport = element('div', 'settings-preview-viewport');
  private readonly frame = element('iframe', 'settings-preview-frame');
  private readonly note = element('span', 'settings-preview-note');
  private readonly orientationButtons: HTMLButtonElement[] = [];
  private portrait = window.innerHeight > window.innerWidth;
  private category = 0;
  private mounted = false;
  private disposed = false;
  private pending = false;
  private rendering = false;
  private raf = 0;
  private observer?: ResizeObserver;
  private clock?: ClockPage;
  private calendar?: CalendarPage;
  private current: typeof prefs = prefs;
  private imageFile: File | null = null;
  private draftUrl: string | null = null;
  private savedUrl: string | null = null;
  private storedImage: Promise<void> | null = null;
  private readonly en = prefs.isClockUseEnglish();

  constructor(private readonly read: () => PreviewDraft) {
    const toolbar = element('div', 'settings-preview-toolbar');
    toolbar.append(element('strong', undefined, this.en ? 'Live preview' : '实时预览'));
    for (const [portrait, label] of [[false, this.en ? 'Landscape' : '横屏'], [true, this.en ? 'Portrait' : '竖屏']] as const) {
      const button = element('button', 'settings-preview-orientation', label);
      button.type = 'button'; button.setAttribute('aria-pressed', String(this.portrait === portrait));
      button.addEventListener('click', () => { this.portrait = portrait; this.orientationButtons.forEach((b,i) => b.setAttribute('aria-pressed', String(Boolean(i) === portrait))); this.fit(); this.request(); });
      this.orientationButtons.push(button); toolbar.append(button);
    }
    this.frame.title = this.en ? 'Unsaved style preview' : '未保存的样式预览';
    this.frame.tabIndex = -1;
    this.viewport.append(this.frame);
    this.element.append(toolbar, this.viewport, this.note);
    localizeUiTree(this.element,prefs.getClockLanguage());
  }

  setCategory(category: number): void {
    this.category = category; this.element.hidden = category > 5;
    this.fit(); this.request();
  }

  mount(): void {
    if (this.mounted || this.disposed || typeof ResizeObserver === 'undefined') return;
    const doc = this.frame.contentDocument;
    if (!doc) return;
    this.mounted = true;
    doc.open(); doc.write('<!doctype html><html><head><style>' + [tokens,fonts,material,base,clock,calendar].join('\n') + '</style></head><body><div id="app"><div id="pages"></div></div></body></html>'); doc.close();
    const parsed = new DOMParser().parseFromString(markup, 'text/html');
    for (const name of ['clock', 'calendar']) doc.getElementById('pages')!.append(doc.importNode(parsed.querySelector('[data-page="' + name + '"]')!, true));
    const settings = new Proxy(prefs, { get: (_target, key) => this.current[key as keyof typeof prefs] });
    this.clock = new ClockPage(doc.querySelector('[data-page="clock"]')!, () => {}, settings, async () => { await this.storedImage; return this.draftUrl ?? this.savedUrl; });
    this.calendar = new CalendarPage(doc.querySelector('[data-page="calendar"]')!, () => {}, settings);
    this.storedImage = readBackgroundBlob().then(blob => { if (blob && !this.disposed) this.savedUrl = URL.createObjectURL(blob); });
    this.observer = new ResizeObserver(() => { this.fit(); this.request(); });
    this.observer.observe(this.viewport);
    this.fit(); this.request();
  }

  request(): void {
    if (!this.mounted || this.disposed || this.element.hidden) return;
    this.pending = true;
    if (!this.raf && !this.rendering) this.raf = requestAnimationFrame(() => { this.raf = 0; void this.flush(); });
  }

  private fit(): void {
    const width = this.portrait ? 576 : 1024, height = this.portrait ? 1024 : 576;
    const scale = Math.min(this.viewport.clientWidth / width, this.viewport.clientHeight / height);
    Object.assign(this.frame.style, { width: width + 'px', height: height + 'px', transform: 'translate(-50%, -50%) scale(' + scale + ')' });
  }

  private async flush(): Promise<void> {
    if (this.rendering || this.disposed) return;
    this.rendering = true;
    try {
      while (this.pending && !this.disposed) {
        this.pending = false;
        const {settings, image} = this.read(); this.current = settings;
        if (image !== this.imageFile) {
          if (this.draftUrl) URL.revokeObjectURL(this.draftUrl);
          this.imageFile = image; this.draftUrl = image ? URL.createObjectURL(image) : null;
        }
        const doc = this.frame.contentDocument!;
        const isCalendar = this.category === 1 || this.category === 5;
        const id = isCalendar ? settings.getUltimateOptions().calendarTheme : settings.getClockTheme();
        const font = settings.getFontFamily(id), weight = settings.getFontWeight(id);
        // Each document owns its font set; load both before the real renderer measures text.
        await Promise.all([ensureFontLoaded(font,weight), ensureFontLoaded(font,weight,doc)]);
        if (this.disposed) break;
        for (const name of ['clock','calendar']) doc.querySelector('[data-page="' + name + '"]')!.classList.toggle('is-active', (name === 'calendar') === isCalendar);
        if (isCalendar) this.calendar!.refreshSettings(); else await this.clock!.refreshSettings();
        this.note.textContent = !isCalendar && settings.getThemeGlass().enabled && settings.getBackgroundMode() !== 'image'
          ? (this.en ? 'Choose an image background to preview glass.' : '选择图片背景后可预览卡片模糊')
          : (this.en ? 'Draft · saved only when applied' : '草稿效果 · 点击应用后保存');
        localizeUiTree(this.element,settings.getClockLanguage());
        this.element.dataset.previewTheme = id;
        this.element.dataset.previewRevision = String(Number(this.element.dataset.previewRevision ?? 0) + 1);
      }
    } catch {
      this.note.textContent = this.en ? 'Preview unavailable. Try another image.' : '预览暂不可用，请尝试其他图片';
    } finally { this.rendering = false; if (this.disposed) this.release(); }
  }

  destroy(): void {
    this.disposed = true; cancelAnimationFrame(this.raf); this.observer?.disconnect();
    if (!this.rendering) this.release();
  }
  private release(): void {
    this.clock?.dispose(); this.clock = undefined; this.calendar?.dispose(); this.calendar = undefined;
    if (this.savedUrl) URL.revokeObjectURL(this.savedUrl);
    if (this.draftUrl) URL.revokeObjectURL(this.draftUrl);
    this.savedUrl = this.draftUrl = null;
  }
}
