package lsfusion.server.data.sql;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.syntax.PostgreSQLSyntax;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.scripted.LocalizedStringOutsideServer;
import lsfusion.server.physics.admin.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// #1903: a long string was inlined into the statement as a literal, a copy for every use of the value
public class InlinedStringTest {

    private Settings settings;

    @BeforeClass
    public static void initLocalizedString() throws ReflectiveOperationException {
        LocalizedStringOutsideServer.init(StringClass.class);
    }

    // the settings a server thread gets from its context, which a unit test has none of
    @Before
    @SuppressWarnings("unchecked")
    public void setSettings() throws ReflectiveOperationException {
        settings = new Settings();
        Field threadSettings = ThreadLocalContext.class.getDeclaredField("settings");
        threadSettings.setAccessible(true);
        ((ThreadLocal<Settings>) threadSettings.get(null)).set(settings);
    }

    @After
    public void dropSettings() {
        ThreadLocalContext.dropSettings();
    }

    private static String parse(String value) {
        String param = SQLSession.getParamName("v");
        return SQLCommand.preparseStatement("SELECT substr(" + param + ", 1, 1), substr(" + param + ", 2, 1)",
                MapFact.<String, ParseInterface>singleton(param, new TypeObject(value, StringClass.text)), PostgreSQLSyntax.instance)
                .getString(PostgreSQLSyntax.instance).getString();
    }

    @Test
    public void stringOfMaxLengthIsInlined() {
        String value = BaseUtils.replicate('a', settings.getMaxInlineStringLength());
        assertTrue(parse(value).contains("'" + value + "'"));
    }

    @Test
    public void longerStringIsPassedAsParameter() {
        String value = BaseUtils.replicate('a', settings.getMaxInlineStringLength() + 1);
        String statement = parse(value);
        assertFalse(statement.contains(value));
        assertTrue(statement.contains("?"));
    }

    @Test
    public void longStringIsInlinedUpToRaisedLimit() {
        settings.setMaxInlineStringLength(Integer.MAX_VALUE);
        String value = BaseUtils.replicate('a', 100000);
        assertTrue(parse(value).contains("'" + value + "'"));
    }
}
