package lsfusion.gwt.server;

import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;

// the cache headers the web server's rules (urlrewrite.xml) give a file it serves
public class FileCacheHeadersTest {

    private static final String RULES = "src/main/webapp/WEB-INF/urlrewrite.xml";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        FileUtils.setTempFolder(folder.newFolder("work").getPath());
    }

    @Test
    public void cachesAnActionFileInTheUsersBrowserOnly() throws Exception { // a file of one user, not kept by a proxy or a CDN for others
        RawFileData report = new RawFileData(("report " + Math.random()).getBytes(StandardCharsets.UTF_8));
        for (String url : new String[]{FileUtils.saveActionFile(report, "pdf", null), FileUtils.saveActionFile(report, "pdf", "report"),
                FileUtils.exportFile(new FileData(report, "xlsx"), null)}) {
            assertTrue(url, url.startsWith(FileUtils.DOWNLOAD_HANDLER + "/" + FileUtils.TEMP_PATH + "/"));

            Map<String, List<String>> headers = headers(url);
            // kept by the browser: the file is deleted after its first read, and "save as" or a second open takes it from there
            assertEquals(url, Collections.singletonList("private, max-age=3600"), headers.get("Cache-Control"));
            assertNull(url, headers.get("Expires")); // a cache that reads no Cache-Control keeps a file by it
        }
    }

    // url as FileUtils returns it: file/<path>?<params>
    private static Map<String, List<String>> headers(String url) throws Exception {
        Conf conf;
        try (InputStream rules = new FileInputStream(RULES)) {
            conf = new Conf(null, rules, RULES, RULES);
            conf.initialise();
        }
        assertTrue(conf.isOk());

        String requestURI = "/" + url.substring(0, url.indexOf('?'));
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(FileCacheHeadersTest.class.getClassLoader(), new Class[]{HttpServletRequest.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getRequestURI": return requestURI;
                case "getContextPath": return "";
                case "getMethod": return "GET";
                case "getCharacterEncoding": return "UTF-8";
                case "getHeaderNames": return Collections.emptyEnumeration();
            }
            return method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? (Object) 0 : null;
        });
        Map<String, List<String>> headers = new HashMap<>();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(FileCacheHeadersTest.class.getClassLoader(), new Class[]{HttpServletResponse.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "addHeader":
                case "addDateHeader":
                    headers.computeIfAbsent((String) args[0], name -> new ArrayList<>()).add(String.valueOf(args[1]));
                    return null;
                case "setHeader":
                case "setDateHeader":
                    headers.put((String) args[0], new ArrayList<>(Collections.singletonList(String.valueOf(args[1]))));
                    return null;
                case "encodeURL":
                case "encodeRedirectURL":
                    return args[0];
            }
            return method.getReturnType() == boolean.class ? false : null;
        });
        new UrlRewriter(conf).processRequest(request, response, (rewrittenRequest, rewrittenResponse) -> {});
        return headers;
    }
}
