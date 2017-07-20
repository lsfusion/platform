package lsfusion.server.logics.scripted;

import org.junit.Test;

import static lsfusion.server.logics.i18n.LocalizedString.escapeForLocalization;
import static org.junit.Assert.assertEquals;

public class EscapeForLocalizationTest {

    @Test
    public void simpleTest() {
        final String source = "simple text";
        final String result = "simple text";
        assertEquals(source, escapeForLocalization(source), result);
    }

    @Test
    public void escapedStringTest() {
        final String source = "\nsimple\ttext\r";
        final String result = "\nsimple\ttext\r";
        assertEquals(source, escapeForLocalization(source), result);
    }

    @Test
    public void escapeBracesTest() {
        final String source = "{test} {} {test}";
        final String result = "\\{test\\} \\{\\} \\{test\\}";
        assertEquals(source, escapeForLocalization(source), result);
   }

    @Test
    public void escapeBackslashesTest() {
        final String source = "\\ test \\ test \\";
        final String result = "\\\\ test \\\\ test \\\\";
        assertEquals(source, escapeForLocalization(source), result);
    }
    
    @Test
    public void alreadyEscapedBracesTest() {
        final String source = "\\{test\\}";
        final String result = "\\\\\\{test\\\\\\}";
        assertEquals(source, escapeForLocalization(source), result);
    }
    
    @Test
    public void nullTest() {
        assertEquals(escapeForLocalization(null), null); 
    }
}