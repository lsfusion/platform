package lsfusion.server.logics.scripted;

import lsfusion.server.logics.i18n.LocalizedString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class TransformLocalizedStringLiteralTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSimple() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'abcd345354_'";
        final String result = "abcd345354_";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testSimple2() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'русский текст'";
        final String result = "русский текст";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testEmpty() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "''";
        final String result = "";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testQuote() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'\\'aaaaaaaa\\'aaaaaaaaaa\\'aaaa\\'\\'aaaaa\\''";
        final String result = "'aaaaaaaa'aaaaaaaaaa'aaaa''aaaaa'";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testBackslashes() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'\\\\aaaaaaaa\\\\aaaaaaaaaa\\\\aaaa\\\\\\\\aaaaa\\\\'";
        final String result = "\\\\aaaaaaaa\\\\aaaaaaaaaa\\\\aaaa\\\\\\\\aaaaa\\\\";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testReturn() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'\\r\\naaaaaaaa\\r\\naaaaaaaaaa\\naaaa\\n\\naaaaa\\r'";
        final String result = "\r\naaaaaaaa\r\naaaaaaaaaa\naaaa\n\naaaaa\r";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testI18n() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'{i18ntext} and not \\{i18ntext\\}'";
        final String result = "{i18ntext} and not \\{i18ntext\\}";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }

    @Test
    public void testI18nError() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'{i18n\ntext} and not \\{i18n\ntext\\}'";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("any whitespace");
        ScriptedStringUtils.transformLocalizedStringLiteral(source);
    }
    
    @Test
    public void testSimpleError() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'\\z'";
        thrown.expect(ScriptedStringUtils.TransformationError.class);
        ScriptedStringUtils.transformLocalizedStringLiteral(source);
    }

    @Test
    public void testAll() throws ScriptedStringUtils.TransformationError, LocalizedString.FormatError {
        final String source = "'\\r\\nbig\\{\\'text\\'\\}message_with\\nescape\\\\\\r'";
        final String result = "\r\nbig\\{'text'\\}message_with\nescape\\\\\r";
        assertEquals(ScriptedStringUtils.transformLocalizedStringLiteral(source).getSourceString(), result);
    }
}