package lsfusion.server.language;

import lsfusion.server.logics.scripted.LocalizedStringOutsideServer;
import lsfusion.server.physics.exec.db.controller.manager.MigrationManager;
import lsfusion.server.physics.exec.db.controller.manager.MigrationVersion;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// the classes of a signature in migration.script are written by their canonical names, the ones the platform writes into
// the canonical name of a property or an action: the parser used to reject some of them, TBOOLEAN, ZDATETIME, intervals and others (#1905)
public class MigrationScriptTest {

    @BeforeClass
    public static void disableCaches() throws ReflectiveOperationException {
        LocalizedStringOutsideServer.setCachesDisabled(true);
    }

    @AfterClass
    public static void enableCaches() throws ReflectiveOperationException {
        LocalizedStringOutsideServer.setCachesDisabled(false);
    }

    private static class Script {
        private final MigrationManager manager = new MigrationManager();
        private final MigrationScriptLexer lexer;
        private final MigrationScriptParser parser;

        private Script(String statement) throws RecognitionException {
            lexer = new MigrationScriptLexer(new ANTLRStringStream("V1.1 {\n    " + statement + "\n}"));
            parser = new MigrationScriptParser(new CommonTokenStream(lexer));
            parser.self = manager;
            parser.script();
        }

        private List<String> errors() {
            List<String> errors = new ArrayList<>(lexer.getErrors());
            errors.addAll(parser.getErrors());
            errors.addAll(manager.getErrors());
            return errors;
        }

        private Map<String, String> propertyChanges() {
            return manager.getPropertyCNChangesAfter(new MigrationVersion("1.0"));
        }
    }

    @Test
    public void testCanonicalClassNames() throws RecognitionException {
        String classes = "INTEGER,DOUBLE,LONG,BOOLEAN,TBOOLEAN,DATE,TIME,DATETIME,ZDATETIME,YEAR," +
                "DATEINTERVAL,DATETIMEINTERVAL,TIMEINTERVAL,ZDATETIMEINTERVAL,STRING,NUMERIC,COLOR," +
                "JSON,JSONTEXT,XML,HTML,TSVECTOR,TSQUERY,FILE,RAWFILE,NAMEDFILE,LINK,RAWLINK";
        Script script = new Script("PROPERTY A.p[" + classes + ",A.Item,?] -> B.p");

        assertEquals(new ArrayList<>(), script.errors());
        assertEquals(new ArrayList<>(), script.manager.getWarnings());
        assertEquals("B.p[" + classes + ",A.Item,?]", script.propertyChanges().get("A.p[" + classes + ",A.Item,?]"));
    }

    @Test
    public void testNotCanonicalClassNames() throws RecognitionException {
        assertEquals(Arrays.asList("line 2:17 TEXT is not a canonical class name: use STRING"), new Script("PROPERTY A.t[TEXT] -> B.t").errors());

        assertError("PROPERTY A.t[ISTRING[50]] -> B.t", "ISTRING[50] is not a canonical class name: use STRING");
        assertError("PROPERTY A.t[DATETIME[3]] -> B.t", "DATETIME[3] is not a canonical class name: use DATETIME");
        assertError("PROPERTY A.t[INTERVAL[DATE]] -> B.t", "INTERVAL[DATE] is not a canonical class name: use DATEINTERVAL");
        assertError("ACTION A.d[Order] -> B.d", "unknown class Order");
    }

    private static void assertError(String statement, String error) throws RecognitionException {
        List<String> errors = new Script(statement).errors();
        assertTrue(errors.toString(), errors.size() == 1 && errors.get(0).contains(error));
    }

    @Test
    public void testSpecificFormatFileClassNames() throws RecognitionException {
        Script script = new Script("PROPERTY A.f[IMAGEFILE,PDFLINK] -> B.f");

        assertEquals(new ArrayList<>(), script.errors());
        assertEquals(Arrays.asList("line 2:17 IMAGEFILE is not a canonical class name, so this change renames nothing: use RAWFILE",
                "line 2:27 PDFLINK is not a canonical class name, so this change renames nothing: use RAWLINK"), script.manager.getWarnings());
        assertEquals("B.f[IMAGEFILE,PDFLINK]", script.propertyChanges().get("A.f[IMAGEFILE,PDFLINK]"));
    }
}
