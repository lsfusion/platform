package lsfusion.server.logics.classes.data.time;

import lsfusion.base.DateConverter;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Pins the offset-bearing-ISO date parsing used by the form controller (GFORM-CONTROLLER-EXEC-EVAL-PLAN §11.1/§12),
 * projected per type WITHOUT timezone normalization for the naive types (DATE/DATETIME/TIME -> local components;
 * ZDATETIME -> instant).
 *
 * Placement (option B), uniform across all 4 types: each {Date,DateTime,Time,ZDateTime}Class.parseFormat tries the
 * locale formatter, then DateConverter.parseOffsetOrNull(s) projected per type (toLocalDate/toLocalDateTime/
 * toLocalTime/toInstant), then its existing smart-parse fallback. The type singletons need a running server, so we
 * test the exact building blocks the 1-line projection uses (parseOffsetOrNull + the JDK projection), plus
 * smartParseInstant which remains ZDATETIME's deeper fallback.
 */
public class DateConverterOffsetTest {

    // 2026-06-01T00:30 local in +03:00 == 2026-05-31T21:30:00Z absolute. Local day (Jun 1) != UTC day (May 31).
    // "seconds, no fraction" — the form the existing ZONED regexps miss, so it relies on the strict OffsetDateTime parse.
    private static final String ISO = "2026-06-01T00:30:00+03:00";

    @Test
    public void dateTakesLocalDateNotUtc() { // DateClass.parseFormat: parseOffsetOrNull(s).toLocalDate()
        assertEquals(LocalDate.of(2026, 6, 1), DateConverter.parseOffsetOrNull(ISO).toLocalDate());
    }

    @Test
    public void dateTimeTakesLocalWallClockNotZoneShifted() { // DateTimeClass.parseFormat: parseOffsetOrNull(s).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 30, 0), DateConverter.parseOffsetOrNull(ISO).toLocalDateTime());
    }

    @Test
    public void timeTakesLocalTime() { // TimeClass.parseFormat: parseOffsetOrNull(s).toLocalTime()
        assertEquals(LocalTime.of(0, 30, 0), DateConverter.parseOffsetOrNull(ISO).toLocalTime());
    }

    @Test
    public void zDateTimeTakesAbsoluteInstant() { // ZDateTimeClass.parseFormat -> DateConverter.smartParseInstant
        assertEquals(Instant.parse("2026-05-31T21:30:00Z"), DateConverter.smartParseInstant(ISO));
    }

    @Test
    public void negativeOffsetInstantAndLocalDiverge() {
        // 2026-06-01T23:30 local in -05:00 == 2026-06-02T04:30Z: local day != UTC day
        String iso = "2026-06-01T23:30:00-05:00";
        assertEquals(LocalDate.of(2026, 6, 1), DateConverter.parseOffsetOrNull(iso).toLocalDate());
        assertEquals(LocalDateTime.of(2026, 6, 1, 23, 30, 0), DateConverter.parseOffsetOrNull(iso).toLocalDateTime());
        assertEquals(Instant.parse("2026-06-02T04:30:00Z"), DateConverter.smartParseInstant(iso));
    }

    @Test
    public void trailingZIsAnOffset() {
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 30, 0), DateConverter.parseOffsetOrNull("2026-06-01T00:30:00Z").toLocalDateTime());
        assertEquals(Instant.parse("2026-06-01T00:30:00Z"), DateConverter.smartParseInstant("2026-06-01T00:30:00Z"));
    }

    @Test
    public void existingSmartParseFormatsUnchanged() { // no regression: smartParse end-fallback was NOT added (option B)
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0, 0), DateConverter.smartParse("2026-06-01")); // atStartOfDay
        assertEquals(LocalDate.of(2026, 6, 1), DateConverter.smartParse("01.06.2026").toLocalDate());
    }

    @Test
    public void parseOffsetOrNullRejectsOffsetless() {
        assertNull(DateConverter.parseOffsetOrNull("2026-06-01T00:30:00")); // offsetless -> falls back to existing parsing
        assertNull(DateConverter.parseOffsetOrNull("2026-06-01"));
    }
}
