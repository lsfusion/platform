package lsfusion.client.classes.data;

import lsfusion.client.view.MainFrame;
import lsfusion.interop.connection.TFormats;
import lsfusion.interop.form.property.cell.IntervalValue;
import org.junit.Test;

import java.time.Instant;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

// The bounds of an INTERVAL[ZDATETIME] are shown in the user's time zone (#2022). A form with such a column failed to open
// in the desktop client: the column's width is measured on a sample interval, whose bounds the zoneless formatter could not
// turn into a day and an hour
public class ClientZDateTimeIntervalClassTest {

    @Test
    public void boundsAreShownInTheUsersTimeZone() {
        TFormats tFormats = MainFrame.tFormats;
        MainFrame.tFormats = new TFormats(null, "dd.MM.yyyy", "HH:mm", TimeZone.getTimeZone("Asia/Yekaterinburg"));
        try {
            IntervalValue interval = new IntervalValue(Instant.parse("2026-11-01T00:00:00Z").toEpochMilli(), Instant.parse("2026-11-30T12:30:00Z").toEpochMilli());
            assertEquals("01.11.2026 05:00 - 30.11.2026 17:30", ClientZDateTimeIntervalClass.instance.formatString(interval));
        } finally {
            MainFrame.tFormats = tFormats;
        }
    }
}
