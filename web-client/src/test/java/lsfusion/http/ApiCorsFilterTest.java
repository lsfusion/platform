package lsfusion.http;

import com.thetransactioncompany.cors.CORSFilter;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static lsfusion.http.Fakes.fake;
import static org.junit.Assert.*;

// The API's CORS filter as web.xml declares it: a request from another origin never gets an answer the browser lets that
// origin read with the user's cookie - no Access-Control-Allow-Credentials, and the origin is not echoed back.
public class ApiCorsFilterTest {

    private static final String WEB_XML = "src/main/webapp/WEB-INF/web.xml";
    private static final String OTHER_ORIGIN = "https://other.example";

    @Test
    public void requestFromAnotherOriginIsNotReadableWithCredentials() throws Exception {
        Map<String, List<String>> headers = filter(request("POST", OTHER_ORIGIN, Collections.emptyMap()));

        assertNoCredentials(headers);
        assertEquals(Collections.singletonList("*"), headers.get("Access-Control-Allow-Origin"));
    }

    @Test
    public void preflightFromAnotherOriginDoesNotAllowCredentials() throws Exception {
        Map<String, String> preflight = new HashMap<>();
        preflight.put("Access-Control-Request-Method", "POST");
        preflight.put("Access-Control-Request-Headers", "content-type");
        Map<String, List<String>> headers = filter(request("OPTIONS", OTHER_ORIGIN, preflight));

        assertNoCredentials(headers);
        assertEquals(Collections.singletonList("*"), headers.get("Access-Control-Allow-Origin"));
        assertTrue("answered as a preflight: " + headers, String.valueOf(headers.get("Access-Control-Allow-Methods")).contains("POST"));
    }

    @Test
    public void nullOriginDoesNotAllowCredentials() throws Exception {
        Map<String, List<String>> headers = filter(request("GET", "null", Collections.emptyMap()));

        assertNoCredentials(headers);
        assertEquals(Collections.singletonList("*"), headers.get("Access-Control-Allow-Origin"));
    }

    private static void assertNoCredentials(Map<String, List<String>> headers) {
        assertNull("Access-Control-Allow-Credentials: " + headers, headers.get("Access-Control-Allow-Credentials"));
        List<String> allowOrigin = headers.get("Access-Control-Allow-Origin");
        if (allowOrigin != null)
            assertFalse("the origin is echoed back: " + headers, allowOrigin.contains(OTHER_ORIGIN));
    }

    private static Map<String, List<String>> filter(HttpServletRequest request) throws Exception {
        CORSFilter filter = new CORSFilter();
        filter.init(filterConfig(corsInitParams()));
        Map<String, List<String>> headers = new HashMap<>();
        filter.doFilter(request, response(headers), (req, resp) -> {});
        return headers;
    }

    private static Map<String, String> corsInitParams() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Path testClasses = Paths.get(ApiCorsFilterTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()); // <module>/target/test-classes
        Document webXml = factory.newDocumentBuilder().parse(testClasses.getParent().getParent().resolve(WEB_XML).toFile());
        NodeList filters = webXml.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (text(filter, "filter-class").equals(CORSFilter.class.getName())) {
                Map<String, String> params = new HashMap<>();
                NodeList initParams = filter.getElementsByTagName("init-param");
                for (int j = 0; j < initParams.getLength(); j++) {
                    Element param = (Element) initParams.item(j);
                    params.put(text(param, "param-name"), text(param, "param-value"));
                }
                return params;
            }
        }
        throw new AssertionError("no " + CORSFilter.class.getName() + " in " + WEB_XML);
    }

    private static String text(Element element, String tag) {
        return element.getElementsByTagName(tag).item(0).getTextContent().trim();
    }

    private static FilterConfig filterConfig(Map<String, String> params) {
        return fake(FilterConfig.class, (name, args) -> {
            switch (name) {
                case "getInitParameter": return params.get((String) args[0]);
                case "getInitParameterNames": return Collections.enumeration(params.keySet());
                case "getFilterName": return "CORS";
                case "getServletContext": return fake(ServletContext.class, (n, a) -> null);
            }
            return null;
        });
    }

    private static HttpServletRequest request(String method, String origin, Map<String, String> extraHeaders) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("Origin", origin);
        headers.put("Host", "app.example");
        headers.put("Cookie", "JSESSIONID=1234");
        headers.putAll(extraHeaders);
        return fake(HttpServletRequest.class, (name, args) -> {
            switch (name) {
                case "getMethod": return method;
                case "getScheme": return "https";
                case "getServerName": return "app.example";
                case "getRequestURI": return "/eval/action";
                case "getHeader": return headers.get((String) args[0]);
                case "getHeaders": {
                    String value = headers.get((String) args[0]);
                    return Collections.enumeration(value != null ? Collections.singletonList(value) : Collections.emptyList());
                }
                case "getHeaderNames": return Collections.enumeration(headers.keySet());
            }
            return null;
        });
    }

    private static HttpServletResponse response(Map<String, List<String>> headers) {
        PrintWriter writer = new PrintWriter(new StringWriter());
        return fake(HttpServletResponse.class, (name, args) -> {
            switch (name) {
                case "addHeader": headers.computeIfAbsent((String) args[0], k -> new ArrayList<>()).add((String) args[1]); break;
                case "setHeader": headers.put((String) args[0], new ArrayList<>(Collections.singletonList((String) args[1]))); break;
                case "getWriter": return writer;
            }
            return null;
        });
    }
}
