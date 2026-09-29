package com.sfood.automation.common.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public final class SqliteInstantParser {

    private static final DateTimeFormatter SQLITE_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .toFormatter();

    private SqliteInstantParser() {
    }

    public static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("T")) {
            return Instant.parse(value);
        }
        return LocalDateTime.parse(value, SQLITE_DATE_TIME_FORMATTER).toInstant(ZoneOffset.UTC);
    }
}
