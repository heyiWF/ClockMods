/** Daily schedules follow Ultimate: all-day first, then time, up to 24 per date. */
export interface ScheduleItem { id: string; title: string; minutes: number | null }
export const MAX_SCHEDULE_ITEMS = 24;
const PREFIX = 'clockmods.schedule.';

export function schedulesFor(date: string): ScheduleItem[] {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(PREFIX + date) ?? '[]');
    if (!Array.isArray(value)) return [];
    return value.filter((item): item is ScheduleItem => item && typeof item.id === 'string'
      && typeof item.title === 'string' && item.title.trim().length > 0
      && (item.minutes === null || (Number.isInteger(item.minutes) && item.minutes >= 0 && item.minutes < 1440)))
      .slice(0, MAX_SCHEDULE_ITEMS).sort((a,b) => (a.minutes ?? -1) - (b.minutes ?? -1));
  } catch { return []; }
}

export function saveSchedule(date: string, title: string, minutes: number | null, id?: string): void {
  title = title.trim();
  if (!title || (minutes !== null && (!Number.isInteger(minutes) || minutes < 0 || minutes >= 1440))) throw new Error('invalid');
  const items = schedulesFor(date), index = items.findIndex(item => item.id === id);
  if (index < 0 && items.length >= MAX_SCHEDULE_ITEMS) throw new Error('limit');
  const item = { id: index >= 0 ? items[index].id : (globalThis.crypto?.randomUUID?.() ?? Date.now().toString(36) + '-' + Math.random().toString(36).slice(2)), title, minutes };
  if (index >= 0) items[index] = item; else items.push(item);
  localStorage.setItem(PREFIX + date, JSON.stringify(items));
}

export function removeSchedule(date: string, id: string): void {
  const items = schedulesFor(date).filter(item => item.id !== id);
  if (items.length) localStorage.setItem(PREFIX + date, JSON.stringify(items));
  else localStorage.removeItem(PREFIX + date);
}
