package lsfusion.http.provider.logics;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.server.FileUtils;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.logics.LogicsConnection;
import lsfusion.interop.logics.LogicsSessionObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

// a request may choose an application server only on the default host or on a host the web server's configuration allows
// (by default the subnets of the computer's own private addresses); a server on another host is not available, and why
// a server is not available is not told
public class LogicsProviderImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final List<LogicsConnection> connections = new ArrayList<>(); // the connections the provider opened

    private final LogicsProviderImpl provider = new LogicsProviderImpl(servletContext()) {
        @Override
        protected LogicsSessionObject createLogicsSessionObject(LogicsConnection connection) throws AppServerNotAvailableException {
            connections.add(connection);
            throw new AppServerNotAvailableException("Application server [" + connection.host + ":" + connection.port + "] is not available. Reason: Connection refused");
        }
    };

    private Map<String, String> initParameters;

    private ServletContext servletContext() {
        return (ServletContext) Proxy.newProxyInstance(LogicsProviderImplTest.class.getClassLoader(), new Class[]{ServletContext.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getInitParameter":
                    return initParameters.get((String) args[0]);
                case "getRealPath":
                    return folder.getRoot().getPath();
                case "getAttribute":
                    return ServletContext.TEMPDIR.equals(args[0]) ? folder.getRoot() : null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private String contextFolder, downloadFolder, uploadFolder; // the web server's folders, set by its configuration

    @Before
    public void setUp() throws Exception {
        contextFolder = FileUtils.APP_CONTEXT_FOLDER_PATH;
        downloadFolder = FileUtils.APP_DOWNLOAD_FOLDER_PATH;
        uploadFolder = FileUtils.APP_UPLOAD_FOLDER_PATH;

        for (String property : new String[]{"app.server", "app.port", "app.export.name", "app.allowed.hosts"}) // they would take the place of the context parameters
            assumeTrue(System.getProperty(property) == null && System.getenv(property.toUpperCase().replace('.', '_')) == null);

        initParameters = new HashMap<>();
        initParameters.put("host", "app-server");
        initParameters.put("port", "7652");
        initParameters.put("exportName", "default");
    }

    private void configure(String allowedHosts) {
        if (allowedHosts != null)
            initParameters.put("allowedHosts", allowedHosts);
        try {
            provider.afterPropertiesSet();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @After
    public void tearDown() {
        FileUtils.APP_CONTEXT_FOLDER_PATH = contextFolder;
        FileUtils.APP_DOWNLOAD_FOLDER_PATH = downloadFolder;
        FileUtils.APP_UPLOAD_FOLDER_PATH = uploadFolder;
    }

    @Test
    public void connectsToTheDefaultServer() {
        configure("app-2, 10.0.1.0/24");
        assertEquals("Application server is not available", run(request()));
        assertConnected("app-server", 7652, "default");
    }

    @Test
    public void connectsToAnAllowedServer() {
        configure("app-2, 10.0.1.0/24");
        run(request("port", "7653", "exportName", "other")); // another server on the default host
        assertConnected("app-server", 7653, "other");

        run(request("host", "app-2"));
        assertConnected("app-2", 7652, "default");

        run(request("host", "app-2", "port", "7700"));
        assertConnected("app-2", 7700, "default");

        run(request("host", "10.0.1.5", "port", "7700"));
        assertConnected("10.0.1.5", 7700, "default");

        run(request("host", "::ffff:10.0.1.5")); // the same address, connected to in its canonical form
        assertConnected("10.0.1.5", 7652, "default");

        run("app-2", null, null);
        assertConnected("app-2", 7652, "default");
    }

    @Test
    public void refusesAServerNotAllowed() {
        configure("app-2, 10.0.1.0/24");
        String[][] notAllowed = {
                {"host", "other-server"},
                {"host", "other-server", "port", "7652", "exportName", "default"},
                {"host", "app-2.other"},
                {"host", "10.0.2.5"},
                {"host", "10.0.1.5.other"}, // a name, not an address of the subnet
                {"port", "0"}, {"port", "-1"}, {"port", "70000"}, // no port
                {"host", "\u0661\u0660.0.1.5"} // digits other than ASCII ones
        };
        for (String[] parameters : notAllowed) {
            HttpServletRequest request = request(parameters);
            String message = run(request);
            assertTrue(String.join(" ", parameters), connections.isEmpty());
            assertEquals("Application server is not available", message);
            assertNull(provider.getServerSettings(request, false));
            assertTrue(String.join(" ", parameters), connections.isEmpty());
        }

        String message = run("other-server", null, null);
        assertTrue(connections.isEmpty());
        assertEquals("Application server is not available", message);

        try {
            provider.runRequest(new LogicsConnection("other-server", 7652, "default"), (sessionObject, retry) -> null);
            fail();
        } catch (AppServerNotAvailableException ignored) {
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        assertTrue(connections.isEmpty());
    }

    @Test
    public void allowsTheLocalSubnetsByDefault() {
        configure(null);
        run(request("host", "127.0.0.1", "port", "7700")); // the loopback interface's address
        assertConnected("127.0.0.1", 7700, "default");

        run(request("host", "127.0.0.2")); // only the loopback interface's own address
        assertTrue(connections.isEmpty());

        run(request("host", "203.0.113.5")); // not a private address
        assertTrue(connections.isEmpty());
    }

    @Test
    public void allowsNoOtherHostsByAnEmptyList() {
        configure("");
        run(request("host", "127.0.0.2"));
        assertTrue(connections.isEmpty());

        run(request("port", "7700"));
        assertConnected("app-server", 7700, "default");
    }

    @Test
    public void logsALineOfLimitedLength() {
        assertEquals("a?b?c?d", LogicsProviderImpl.getLogLine("a\nb\rc\u2028d"));
        String line = LogicsProviderImpl.getLogLine(new String(new char[600]).replace('\0', 'x'));
        assertEquals(500, line.length());
        assertTrue(line.endsWith("..."));
    }

    // runs a request, returns the message of the not available exception it ends with
    private String run(HttpServletRequest request) {
        try {
            provider.runRequest(request, (sessionObject, retry) -> null);
        } catch (AppServerNotAvailableDispatchException e) {
            return e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("no application server in a unit test");
    }

    private String run(String host, Integer port, String exportName) { // the way the web client's own requests choose a server
        try {
            provider.runRequest(host, port, exportName, (sessionObject, retry) -> null);
        } catch (AppServerNotAvailableDispatchException e) {
            return e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("no application server in a unit test");
    }

    private void assertConnected(String host, int port, String exportName) {
        assertEquals(1, connections.size());
        LogicsConnection connection = connections.remove(0);
        assertEquals(host, connection.host);
        assertEquals(port, connection.port);
        assertEquals(exportName, connection.exportName);
    }

    private static HttpServletRequest request(String... parameters) {
        Map<String, String> parameterMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i += 2)
            parameterMap.put(parameters[i], parameters[i + 1]);
        return (HttpServletRequest) Proxy.newProxyInstance(LogicsProviderImplTest.class.getClassLoader(), new Class[]{HttpServletRequest.class}, (proxy, method, args) -> {
            if (method.getName().equals("getParameter"))
                return parameterMap.get((String) args[0]);
            throw new UnsupportedOperationException(method.getName());
        });
    }
}
