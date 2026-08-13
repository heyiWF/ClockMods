/** Ported from com.clockmods.ui.ClockTimeFormatter. */

export interface DisplayTime {
  mainText: string;
  secondsText: string;
  periodText: string;
  colonVisible: boolean;
}

export function hasSmallSeconds(time: DisplayTime): boolean {
  return time.secondsText.length > 0;
}

export function hasPeriod(time: DisplayTime): boolean {
  return time.periodText.length > 0;
}

export function formatTime(
  hour: number,
  minute: number,
  second: number,
  showSeconds: boolean,
  blinkColon: boolean,
  smallSeconds: boolean,
  use24Hour: boolean,
  useEnglish: boolean
): DisplayTime {
  const showColon = !blinkColon || second % 2 === 0;
  const separator = ':';
  let displayHour = use24Hour ? hour : hour % 12;
  if (!use24Hour && displayHour === 0) displayHour = 12;
  const periodText = use24Hour ? '' : periodTextFor(hour, useEnglish);
  const hoursAndMinutes = twoDigits(displayHour) + separator + twoDigits(minute);
  if (!showSeconds) {
    return { mainText: hoursAndMinutes, secondsText: '', periodText, colonVisible: showColon };
  }
  if (smallSeconds) {
    return {
      mainText: hoursAndMinutes,
      secondsText: twoDigits(second),
      periodText,
      colonVisible: showColon,
    };
  }
  return {
    mainText: hoursAndMinutes + separator + twoDigits(second),
    secondsText: '',
    periodText,
    colonVisible: showColon,
  };
}

export function formatHourlyChime(
  hour: number,
  minute: number,
  use24Hour: boolean,
  useEnglish: boolean
): string {
  if (use24Hour) return twoDigits(hour) + ':' + twoDigits(minute);
  let displayHour = hour % 12;
  if (displayHour === 0) displayHour = 12;
  const time = displayHour + ':' + twoDigits(minute);
  const period = periodTextFor(hour, useEnglish);
  return useEnglish ? time + ' ' + period : period + time;
}

export function periodTextFor(hour: number, useEnglish: boolean): string {
  if (useEnglish) return hour < 12 ? 'AM' : 'PM';
  return hour < 12 ? '上午' : '下午';
}

/** `HH:mm` used by the settings time pickers and the quiet-hours labels. */
export function formatMinutesOfDay(minutes: number): string {
  return twoDigits(Math.floor(minutes / 60)) + ':' + twoDigits(minutes % 60);
}

export function twoDigits(value: number): string {
  return String(value).padStart(2, '0');
}
