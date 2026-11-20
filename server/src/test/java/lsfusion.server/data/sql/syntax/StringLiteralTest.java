package lsfusion.server.data.sql.syntax;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

// a string written into the SQL text as a literal keeps every character it holds, a quote and a backslash included
public class StringLiteralTest {

    @Test
    public void plainString() {
        assertEquals("'abc'", PostgreSQLSyntax.instance.getStringLiteral("abc"));
        assertEquals("'abc'", MSSQLSQLSyntax.instance.getStringLiteral("abc"));
    }

    @Test
    public void quote() {
        assertEquals("'it''s'", PostgreSQLSyntax.instance.getStringLiteral("it's"));
        assertEquals("'a''b''c'", PostgreSQLSyntax.instance.getStringLiteral("a'b'c"));
        assertEquals("'it''s'", MSSQLSQLSyntax.instance.getStringLiteral("it's"));
    }

    @Test
    public void backslash() {
        // E'' reads a backslash as an escape whatever standard_conforming_strings is
        assertEquals("E'a\\\\b'", PostgreSQLSyntax.instance.getStringLiteral("a\\b"));
        assertEquals("E'a\\\\''b'", PostgreSQLSyntax.instance.getStringLiteral("a\\'b"));
    }
}
