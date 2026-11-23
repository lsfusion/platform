package lsfusion.tests;

import org.junit.Assert;
import org.junit.Test;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Arrays;

import static lsfusion.tests.TestServer.BASE;
import static lsfusion.tests.TestServer.excerpt;
import static lsfusion.tests.TestServer.read;

/** The module LogicsExtendCode, its code kept in SystemEvents.logicsExtendCode[], on a server of its own : a code is
 *  saved, and the server is started again on the same database to compile it with the logics. The logics is the
 *  platform's and a module of calls, its top module (startextendcode/StartCodeCalls.lsf); the modules included have
 *  one it does not require besides (StartCodeOutside.lsf), which the code requires. target/classes is there for
 *  ReadInlineFilesAction. */
public class StartExtendCodeIT {

    private static final String DATABASE = "lsfusion_test_start_code_" + ProcessHandle.current().pid();

    private static final String STARTED = "Server has successfully started";

    // what only a module of the logics declares : a class with a table of its own, data stored, an action added to the
    // start actions, counting the starts. Besides : an action, a literal naming a resource, and a folder of the navigator,
    // which the code of a connection moves. The code begins with REQUIRE and PRIORITY, which go on the header the platform
    // writes : it requires a module the top module does not, and calls an action of it
    private static final String START_CODE = "REQUIRE StartCodeOutside;\n"
            + "PRIORITY StartCodeCalls;\n"
            + "CLASS SccThing 'Thing';\n"
            + "TABLE sccThing (SccThing);\n"
            + "sccName = DATA STRING[50] (SccThing);\n"
            + "sccStarts = DATA INTEGER ();\n"
            + "onStarted() + { sccStarts() <- (OVERRIDE sccStarts(), 0) + 1; }\n"
            + "sccStartsValue() { RETURN sccStarts(); }\n"
            + "sccAddThing(STRING[50] name) { NEW t = SccThing { sccName(t) <- name; } APPLY; }\n"
            + "sccThings() { RETURN GROUP CONCAT sccName(SccThing t), ',' ORDER t; }\n"
            + "sccTwice(INTEGER a) { RETURN a * 2; }\n"
            + "sccFromOutside() { RETURN sccOutside(); }\n"
            + "sccResource() = 'a$R{StartCodeResource.txt}b';\n"
            + "NAVIGATOR { system { NEW FOLDER sccFolder 'Start code'; } }";
    private static final String CONNECTION_CODE = "sccUses() { RETURN sccTwice(5); }\n"
            + "NAVIGATOR { administration { MOVE sccFolder; } }";

    private Process server;
    private int httpPort;

    @Test
    public void theLogicsIsBuiltWithTheCode() throws Exception {
        try {
            start(1);
            // the module LogicsExtendCode, with no code, goes over the modules of the logics, not over every module of
            // the classpath
            assertNotFound("sccOutside");
            call("StartCodeCalls.sccSetLogics&p=" + URLEncoder.encode(START_CODE, "UTF-8"));
            // saved, and not compiled till the next start : nor is the resource a literal of it names read
            assertNotFound("sccTwice&p=21");
            Assert.assertEquals("a resource a literal of a code only saved names", "not found", call("StartCodeCalls.sccResourceNamed"));

            restart(2);
            // the literal is the module's : its resource is found as the module is compiled, though nothing has read it
            Assert.assertEquals("a resource a literal of the code names", "resource StartCodeResource.txt", call("StartCodeCalls.sccResourceNamed"));
            // in the namespace of the module, the same at every start, and by the bare name
            Assert.assertEquals("an action of the code", "42", call("LogicsExtendCode.sccTwice&p=21"));
            Assert.assertEquals("an action of the code by its name", "42", call("sccTwice&p=21"));
            Assert.assertEquals("the start actions the code adds to", "1", call("sccStartsValue"));
            // a module the code requires is loaded, though the top module does not require it
            Assert.assertEquals("an action of a module the code requires", "outside", call("sccOutside"));
            Assert.assertEquals("an action of the code calling the module it requires", "outside", call("sccFromOutside"));
            call("sccAddThing&p=first");
            Assert.assertEquals("an object of a class of the code", "first", call("sccThings"));

            // the code of a connection sees what the module declares, and the navigator it copies has the module's
            // folder. A call takes the code its session is built with : a session of the stateful API of its own,
            // built after the code is saved, not one of the pool
            call("StartCodeCalls.sccSetConnection&p=" + URLEncoder.encode(CONNECTION_CODE, "UTF-8"));
            Assert.assertEquals("an action of the connection's code", "10", call("sccUses&session=scc_close"));
            call("StartCodeCalls.sccSetConnection");

            // what the module stores is kept from start to start, as a module's is
            restart(3);
            Assert.assertEquals("an object of a class of the code, after a start", "first", call("sccThings"));
            Assert.assertEquals("the start actions the code adds to, after a start", "2", call("sccStartsValue"));

            // a code that does not compile is saved, and stops the next start as an error in any module does : the
            // error names the module, at the line of the code
            call("StartCodeCalls.sccSetLogics&p=" + URLEncoder.encode("sccTwice(INTEGER a) { RETURN a * 2; }\nsccBroken( {", "UTF-8"));
            TestServer.kill(server);
            server = launch(4);
            Process failed = server;
            TestServer.waitFor(() -> !failed.isAlive() || read(log(4)).contains(STARTED), failed, log(4),
                    "the server neither started nor stopped");
            String output = read(log(4));
            Assert.assertFalse("started with a code that does not compile\n" + excerpt(output), output.contains(STARTED));
            Assert.assertTrue("the error does not name the module at the line of the code\n" + excerpt(output),
                    output.contains("LogicsExtendCode:2:"));
        } finally {
            if (server != null)
                TestServer.stop(server, DATABASE);
        }
    }

    private Process launch(int number) throws Exception {
        int[] ports = TestServer.freePorts(4);
        httpPort = ports[0];
        return TestServer.start(DATABASE, ports, log(number), Arrays.asList(BASE.resolve("target/classes"), BASE.resolve("startextendcode")),
                "logics.includePaths=/StartCodeCalls.lsf;/StartCodeOutside.lsf", "logics.topModule=StartCodeCalls", "settings.enableAPI=2");
    }

    private void start(int number) throws Exception {
        server = launch(number);
        TestServer.waitStarted(server, log(number));
    }

    // on the same database
    private void restart(int number) throws Exception {
        TestServer.kill(server);
        server = null;
        start(number);
    }

    private static Path log(int number) {
        return BASE.resolve("target/server-start-code-" + number + ".log");
    }

    private String call(String action) throws Exception {
        HttpResponse<String> response = post(action);
        Assert.assertEquals(action + " answered " + response.body(), 200, response.statusCode());
        return response.body();
    }

    private void assertNotFound(String action) throws Exception {
        HttpResponse<String> response = post(action);
        Assert.assertTrue(action + " answered " + response.statusCode() + " " + response.body(),
                response.statusCode() != 200 && response.body().contains("was not found"));
    }

    private HttpResponse<String> post(String action) throws Exception {
        return TestServer.post(httpPort, "/exec?action=" + action, "", "text/plain");
    }
}
