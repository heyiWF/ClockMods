/** @vitest-environment jsdom */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MAX_SCHEDULE_ITEMS, removeSchedule, saveSchedule, schedulesFor } from '../src/core/schedule-store';
beforeEach(() => localStorage.clear());
describe('daily schedules', () => {
 it('persists by civil date and orders all-day before timed items', () => {
  saveSchedule('2026-10-10','Late',900); saveSchedule('2026-10-10','All day',null); saveSchedule('2026-10-10','Early',510);
  expect(schedulesFor('2026-10-10').map(x=>x.title)).toEqual(['All day','Early','Late']);
  expect(schedulesFor('2026-10-11')).toEqual([]);
 });
 it('edits by stable ID and removes without changing another date', () => {
  saveSchedule('2026-10-10','First',null); saveSchedule('2026-10-11','Other',null);
  const id=schedulesFor('2026-10-10')[0].id;saveSchedule('2026-10-10',' Updated ',600,id);
  expect(schedulesFor('2026-10-10')).toEqual([{id,title:'Updated',minutes:600}]);
  removeSchedule('2026-10-10',id);expect(schedulesFor('2026-10-10')).toEqual([]);expect(schedulesFor('2026-10-11')).toHaveLength(1);
 });
 it('limits new items but permits editing a full day', () => {
  for(let i=0;i<MAX_SCHEDULE_ITEMS;i++)saveSchedule('2026-10-10',String(i),null);
  expect(()=>saveSchedule('2026-10-10','Overflow',null)).toThrow('limit');
  const id=schedulesFor('2026-10-10')[0].id;saveSchedule('2026-10-10','Changed',30,id);
  expect(schedulesFor('2026-10-10')).toHaveLength(MAX_SCHEDULE_ITEMS);
 });
 it('rejects blank titles and invalid times, and tolerates corrupt storage', () => {
  expect(()=>saveSchedule('2026-10-10',' ',null)).toThrow('invalid');
  for(const time of [-1,1440,1.5,NaN])expect(()=>saveSchedule('2026-10-10','Title',time)).toThrow('invalid');
  localStorage.setItem('clockmods.schedule.2026-10-10','invalid');expect(schedulesFor('2026-10-10')).toEqual([]);
 });
 it('propagates storage failure so the editor can retain the draft', () => {
  const spy=vi.spyOn(Storage.prototype,'setItem').mockImplementation(()=>{throw new Error('quota');});
  expect(()=>saveSchedule('2026-10-10','Draft',null)).toThrow('quota');spy.mockRestore();
 });
});
