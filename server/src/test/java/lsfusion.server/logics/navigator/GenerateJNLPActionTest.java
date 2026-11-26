package lsfusion.server.logics.navigator;

import org.junit.Test;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class GenerateJNLPActionTest {

    @Test
    public void testLastModifiedOnRussianServer() {
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        TimeZone timeZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.Category.FORMAT, new Locale("ru", "RU"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Minsk"));

            assertEquals("Sun, 20 Jan 1963 14:00:43 GMT", GenerateJNLPAction.getLastModified("client.jnlp"));
            assertEquals("Sat, 07 Feb 1970 19:30:16 GMT", GenerateJNLPAction.getLastModified("jnlp"));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, locale);
            TimeZone.setDefault(timeZone);
        }
    }
}
