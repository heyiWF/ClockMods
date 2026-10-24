package com.clockmods.calendar.imports;

import java.io.IOException;

public final class CalendarImportException extends IOException {
    public final String reason;
    public CalendarImportException(String reason) { super(reason); this.reason = reason; }
}
