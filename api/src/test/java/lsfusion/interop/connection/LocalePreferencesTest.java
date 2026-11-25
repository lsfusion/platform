package lsfusion.interop.connection;

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

// #2017: a user time zone written as an offset (UTC+5) was taken as UTC by the server and the clients, and as 5 hours west of UTC by the database
public class LocalePreferencesTest {

    private static String timeZone(String userTimeZone) {
        return new LocalePreferences(null, userTimeZone, null, null, null).timeZone;
    }

    private static void assertEast(String userTimeZone, int hours) {
        String timeZone = timeZone(userTimeZone);
        assertEquals(userTimeZone + " -> " + timeZone, hours * 3600_000, TimeZone.getTimeZone(timeZone).getRawOffset());
    }

    @Test
    public void offsetsAreEastOfUTC() {
        assertEast("UTC+5", 5);
        assertEast("GMT+5", 5);
        assertEast("+05:00", 5);
        assertEast("utc+5", 5);
        assertEast("GMT-8:00", -8); // the format of TimeZone.getTimeZone, as the docs suggest
        assertEast("UTC-3", -3);
        assertEast("UTC+14", 14);
        assertEast("UTC+0", 0);
    }

    @Test
    public void offsetsBecomeEtcZones() { // the database reads Etc/GMT-5 as 5 hours east of UTC, as the clients do, and UTC+5 as 5 hours west
        assertEquals("Etc/GMT-5", timeZone("UTC+5"));
        assertEquals("Etc/GMT+3", timeZone("GMT-3"));
        assertEquals("Etc/UTC", timeZone("+00:00"));
    }

    @Test
    public void otherTimeZonesAreKept() {
        assertEquals("Asia/Yekaterinburg", timeZone("Asia/Yekaterinburg"));
        assertEquals("UTC+05:30", timeZone("UTC+05:30")); // no Etc zone for it
        assertEquals("unknown", timeZone("unknown"));
        assertEquals(null, timeZone(null));
    }
}
