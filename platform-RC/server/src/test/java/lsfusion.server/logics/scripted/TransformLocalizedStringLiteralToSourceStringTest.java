package lsfusion.server.logics.scripted;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static lsfusion.server.logics.scripted.ScriptedStringUtils.transformLocalizedStringLiteralToSourceString;
import static org.junit.Assert.assertEquals;

public class TransformLocalizedStringLiteralToSourceStringTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSimple() throws ScriptedStringUtils.TransformationError {
        final String source = "'abcd345354_'";
        final String result = "abcd345354_";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testSimple2() throws ScriptedStringUtils.TransformationError {
        final String source = "'русский текст'";
        final String result = "русский текст";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testEmpty() throws ScriptedStringUtils.TransformationError {
        final String source = "''";
        final String result = "";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testQuote() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\'aaaaaaaa\\'aaaaaaaaaa\\'aaaa\\'\\'aaaaa\\''";
        final String result = "'aaaaaaaa'aaaaaaaaaa'aaaa''aaaaa'";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testBackslashes() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\\\aaaaaaaa\\\\aaaaaaaaaa\\\\aaaa\\\\\\\\aaaaa\\\\'";
        final String result = "\\\\aaaaaaaa\\\\aaaaaaaaaa\\\\aaaa\\\\\\\\aaaaa\\\\";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testReturn() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\r\\naaaaaaaa\\r\\naaaaaaaaaa\\naaaa\\n\\naaaaa\\r'";
        final String result = "\r\naaaaaaaa\r\naaaaaaaaaa\naaaa\n\naaaaa\r";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testI18n() throws ScriptedStringUtils.TransformationError {
        final String source = "'{i18ntext} and not \\{i18ntext\\}'";
        final String result = "{i18ntext} and not \\{i18ntext\\}";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }

    @Test
    public void testSimpleError() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\z'";
        thrown.expect(ScriptedStringUtils.TransformationError.class);
        transformLocalizedStringLiteralToSourceString(source);
    }

    @Test
    public void testAll() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\r\\nbig\\{\\'text\\'\\}message_{with}\\tescape\\\\\\r'";
        final String result = "\r\nbig\\{'text'\\}message_{with}\tescape\\\\\r";
        assertEquals(transformLocalizedStringLiteralToSourceString(source), result);
    }
    
    @Test 
    public void testNull() throws ScriptedStringUtils.TransformationError {
        assertEquals(transformLocalizedStringLiteralToSourceString(null), null); 
    }

    @Test
    public void testLastBackslash() throws ScriptedStringUtils.TransformationError {
        final String source = "'text\\'";
        thrown.expect(ScriptedStringUtils.TransformationError.class);
        transformLocalizedStringLiteralToSourceString(source);
    }

}