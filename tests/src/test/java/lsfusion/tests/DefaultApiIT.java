package lsfusion.tests;

import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.nio.file.Path;

import static lsfusion.tests.TestServer.BASE;

/** The classes of a server with the API's default access (enableAPI=0), on one server : its start is the most of what
 *  each of them would cost on a server of its own, and the settings one of them adds change nothing another asserts.
 *  Failsafe runs them only by this suite (see the pom), one after another in one fork; a class run alone, with
 *  -Dit.test, starts the server itself. The server is started by the first class that needs it and stopped once the
 *  suite ends, or with the jvm. The order matters : LockedUserIT first, as it reads the user of the calls without a login
 *  as a new database has it, PasswordResetIT last, as it points the server's mails at a mail server it closes. */
@RunWith(Suite.class)
@Suite.SuiteClasses({LockedUserIT.class, ExternalApiDisabledIT.class, PasswordResetIT.class})
public class DefaultApiIT {

    private static final String DATABASE = "lsfusion_test_api0_" + ProcessHandle.current().pid();

    private static Process server;
    static int httpPort, rmiPort;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    static synchronized void start() throws Exception {
        if (server == null)
            start("target/server-api0.log");
    }

    // LockedUserIT's start of the server again, on the same database
    static synchronized void restart(String logName) throws Exception {
        TestServer.kill(server);
        start(logName);
    }

    private static void start(String logName) throws Exception {
        int[] ports = TestServer.freePorts(4);
        Path log = BASE.resolve(logName);
        server = TestServer.start(DATABASE, ports, log, BASE.resolve("target/classes"),
                "settings.enableAPI=0",
                // ExternalApiDisabledIT : a language whose names of days and months are not Latin (#2024: the link to the
                // desktop client wrote its Last-Modified in it, which the HTTP port of the server refused)
                "user.setLanguage=ru",
                // ExternalApiDisabledIT, LockedUserIT : the periodic drop of the values the server keeps, which a change
                // does not drop itself : run as the server starts, then an hour later
                "settings.flushAsyncValuesCaches=3600",
                // LockedUserIT : which lets an administrator in whatever password they have
                "settings.universalPassword=" + LockedUserIT.UNIVERSAL_PASSWORD,
                "lsfusion.server.devmode=false"); // which would set enableAPI to 2 whatever is given
        try {
            TestServer.waitStarted(server, log);
        } catch (Exception e) { // the next class tries a start of its own, rather than call a server that is not there
            stop();
            throw e;
        }
        httpPort = ports[0];
        rmiPort = ports[1];
    }

    @AfterClass
    public static synchronized void stop() throws Exception {
        if (server != null) {
            Process stopped = server;
            server = null;
            TestServer.stop(stopped, DATABASE);
        }
    }
}
