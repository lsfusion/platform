package lsfusion.server.logics.scripted;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class TransformStringLiteralTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test 
    public void testSimple() throws ScriptedStringUtils.TransformationError {
        final String source = "'abcd345354_'";
        final String result = "abcd345354_";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }

    @Test
    public void testSimple2() throws ScriptedStringUtils.TransformationError {
        final String source = "'русский текст'";
        final String result = "русский текст";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }
    
    @Test
    public void testEmpty() throws ScriptedStringUtils.TransformationError {
        final String source = "''";
        final String result = "";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }
    
    @Test
    public void testQuote() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\'aaaaaaaa\\'aaaaaaaaaa\\'aaaa\\'\\'aaaaa\\''";
        final String result = "'aaaaaaaa'aaaaaaaaaa'aaaa''aaaaa'";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }

    @Test
    public void testBackslashes() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\\\aaaaaaaa\\\\aaaaaaaaaa\\\\aaaa\\\\\\\\aaaaa\\\\'";
        final String result = "\\aaaaaaaa\\aaaaaaaaaa\\aaaa\\\\aaaaa\\";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }

    @Test
    public void testReturn() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\r\\naaaaaaaa\\r\\naaaaaaaaaa\\naaaa\\n\\naaaaa\\r'";
        final String result = "\r\naaaaaaaa\r\naaaaaaaaaa\naaaa\n\naaaaa\r";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }
    
    @Test
    public void testI18nError() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\{text\\}'";
        thrown.expect(ScriptedStringUtils.TransformationError.class);
        thrown.expectMessage("no localization");
        ScriptedStringUtils.transformStringLiteral(source);
    }

    @Test
    public void testSimpleError() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\z'";
        thrown.expect(ScriptedStringUtils.TransformationError.class);
        ScriptedStringUtils.transformStringLiteral(source);
    }
    
    @Test 
    public void testAll() throws ScriptedStringUtils.TransformationError {
        final String source = "'\\r\\nbig\\'text\\'message_with\\nescape\\\\\\r'";
        final String result = "\r\nbig'text'message_with\nescape\\\r";
        assertEquals(ScriptedStringUtils.transformStringLiteral(source), result);
    }
}