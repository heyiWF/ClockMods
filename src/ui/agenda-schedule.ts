import { t } from '../core/i18n';
import { MAX_SCHEDULE_ITEMS, removeSchedule, saveSchedule, schedulesFor } from '../core/schedule-store';
import type { ScheduleItem } from '../core/schedule-store';
import { openTimePicker } from './time-picker';

const timeLabel = (minutes: number) => String(Math.floor(minutes / 60)).padStart(2, '0') + ':' + String(minutes % 60).padStart(2, '0');
function button(label: string, className = 'm3-button m3-button--text'): HTMLButtonElement {
  const node = document.createElement('button'); node.type = 'button'; node.className = className; node.textContent = label; return node;
}

export function agendaSchedule(date: string, readOnly = false): HTMLElement {
  const pane = document.createElement('section'); pane.className = 'cal-agenda-schedule no-page-swipe';
  const render = () => {
    const header = document.createElement('div'); header.className = 'cal-schedule-header';
    const heading = document.createElement('h3'); heading.textContent = t('schedule_title');
    const add = button(t('schedule_add')); add.classList.add('cal-schedule-add'); add.disabled = readOnly;
    add.onclick = () => openEditor(); header.append(heading, add);
    const list = document.createElement('div'); list.className = 'cal-schedule-list';
    const items = schedulesFor(date);
    if (!items.length) { const empty = document.createElement('p'); empty.className = 'cal-schedule-empty'; empty.textContent = t('schedule_empty'); list.append(empty); }
    for (const item of items) {
      const row = button('', 'cal-schedule-row'); row.disabled = readOnly;
      const time = document.createElement('span'); time.className = 'cal-schedule-time' + (item.minutes === null ? ' is-all-day' : ''); time.textContent = item.minutes === null ? t('schedule_all_day') : timeLabel(item.minutes);
      const title = document.createElement('span'); title.className = 'cal-schedule-item-title'; title.textContent = item.title;
      row.append(time, title); row.onclick = () => openEditor(item); list.append(row);
    }
    pane.replaceChildren(header, list);
  };
  const openEditor = (existing?: ScheduleItem) => {
    const dialog = document.createElement('dialog'); dialog.className = 'timer-dialog schedule-dialog no-page-swipe';
    dialog.setAttribute('aria-label', t(existing ? 'schedule_edit' : 'schedule_add'));
    const form = document.createElement('form');
    const heading = document.createElement('h2'); heading.textContent = t(existing ? 'schedule_edit' : 'schedule_add');
    const day = document.createElement('p'); day.className = 'schedule-editor-date'; day.textContent = date;
    const field = document.createElement('label'); field.className = 'timer-dialog-field';
    const caption = document.createElement('span'); caption.textContent = t('schedule_name');
    const title = document.createElement('input'); title.type = 'text'; title.required = true; title.value = existing?.title ?? ''; title.name = 'schedule-title';
    title.setAttribute('aria-label', t('schedule_name')); field.append(caption, title);
    let minutes = existing?.minutes ?? null;
    const timeRow = document.createElement('div'); timeRow.className = 'schedule-time-row';
    const time = button(''); time.classList.add('schedule-time'); time.setAttribute('aria-label', t('schedule_time'));
    const allDay = button(t('schedule_all_day')); allDay.classList.add('schedule-all-day');
    const refreshTime = () => { time.textContent = minutes === null ? t('schedule_choose_time') : timeLabel(minutes); allDay.setAttribute('aria-pressed', String(minutes === null)); };
    time.onclick = () => openTimePicker(minutes ?? 9 * 60, t('schedule_time'), value => { minutes = value; refreshTime(); });
    allDay.onclick = () => { minutes = null; refreshTime(); }; refreshTime(); timeRow.append(time, allDay);
    const error = document.createElement('p'); error.className = 'schedule-error'; error.setAttribute('role', 'alert'); error.hidden = true;
    const actions = document.createElement('div'); actions.className = 'timer-dialog-actions';
    const cancel = button(t('cancel')); cancel.onclick = () => dialog.close();
    const save = button(t('schedule_save'), 'm3-button m3-button--filled'); save.type = 'submit';
    const fail = (cause: unknown) => { error.hidden = false; error.textContent = cause instanceof Error && cause.message === 'limit' ? t('schedule_limit', MAX_SCHEDULE_ITEMS) : t('schedule_save_error'); };
    if (existing) {
      const remove = button(t('schedule_delete')); remove.classList.add('schedule-delete');
      remove.onclick = () => { try { removeSchedule(date, existing.id); render(); dialog.close(); } catch(cause) { fail(cause); } }; actions.append(remove);
    }
    actions.append(cancel, save);
    form.append(heading, day, field, timeRow, error, actions); dialog.append(form);
    title.oninput = () => title.setCustomValidity('');
    form.onsubmit = event => {
      event.preventDefault(); title.setCustomValidity(title.value.trim() ? '' : t('schedule_name_required'));
      if (!form.reportValidity()) return;
      try { saveSchedule(date, title.value, minutes, existing?.id); render(); dialog.close(); } catch(cause) { fail(cause); }
    };
    dialog.addEventListener('close', () => { dialog.remove(); pane.querySelector<HTMLButtonElement>('.cal-schedule-add')?.focus(); }, {once:true});
    document.body.append(dialog); dialog.showModal(); title.focus();
  };
  render(); return pane;
}
