package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.server.data.sql.syntax.PostgreSQLSyntax;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.scripted.LocalizedStringOutsideServer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// #1903: a long string was inlined into the statement as a literal, a copy for every use of the value
public class InlinedStringTest {

    @BeforeClass
    public static void initLocalizedString() throws ReflectiveOperationException {
        LocalizedStringOutsideServer.init(StringClass.class);
    }

    private static String parse(String value) {
        String param = SQLSession.getParamName("v");
        return SQLCommand.preparseStatement("SELECT substr(" + param + ", 1, 1), substr(" + param + ", 2, 1)",
                MapFact.<String, ParseInterface>singleton(param, new TypeObject(value, StringClass.text)), PostgreSQLSyntax.instance)
                .getString(PostgreSQLSyntax.instance).getString();
    }

    @Test
    public void shortStringIsInlined() {
        assertTrue(parse("abcdefghij").contains("'abcdefghij'"));
    }

    @Test
    public void longStringIsPassedAsParameter() {
        String value = BaseUtils.replicate('a', 100000);
        String statement = parse(value);
        assertFalse(statement.contains(value));
        assertTrue(statement.contains("?"));
    }
}
