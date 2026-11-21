package lsfusion.http.authentication;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.http.Fakes.fake;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

// Which requests LSFCrossSiteRequestFilter runs on a logged-in session: the ones a page of the web client sends, as
// the browser tells by Sec-Fetch-Site, else by the Origin, else by the Referer. The request is a fake of the web
// client at web:8080
public class LSFCrossSiteRequestFilterTest {

    private static final String WEB = "http://web:8080";

    // the address bar, a bookmark, a link another application hands to the browser
    @Test
    public void userOpenedRequestRefused() {
        assertFalse(isAllowed("GET", "Sec-Fetch-Site", "none"));
        assertFalse(isAllowed("POST", "Sec-Fetch-Site", "none"));
    }

    @Test
    public void ownPageRequestAllowed() {
        assertTrue(isAllowed("GET", "Sec-Fetch-Site", "same-origin"));
        assertTrue(isAllowed("POST", "Sec-Fetch-Site", "same-origin", "Origin", WEB));
    }

    @Test
    public void otherPageRequestRefused() {
        assertFalse(isAllowed("GET", "Sec-Fetch-Site", "cross-site"));
        assertFalse(isAllowed("GET", "Sec-Fetch-Site", "same-site", "Referer", WEB + "/main"));
    }

    // over plain http to a host other than localhost a browser sends no Sec-Fetch-Site
    @Test
    public void originDecidesWithoutSecFetchSite() {
        assertTrue(isAllowed("POST", "Origin", WEB));
        assertFalse(isAllowed("POST", "Origin", "http://other:8080"));
        assertFalse(isAllowed("POST", "Origin", "null"));
    }

    // a link of a page of the web client over plain http to a host other than localhost: neither Sec-Fetch-Site nor
    // Origin, the page in the Referer
    @Test
    public void refererDecidesWithoutOrigin() {
        assertTrue(isAllowed("GET", "Referer", WEB + "/main"));
        assertTrue(isAllowed("HEAD", "Referer", WEB + "/main"));
        assertFalse(isAllowed("GET", "Referer", "http://other:8080/main"));
        assertFalse(isAllowed("POST", "Referer", "http://other:8080/main"));
        assertFalse(isAllowed("GET"));
    }

    // a client other than a browser
    @Test
    public void undescribedPostAllowed() {
        assertTrue(isAllowed("POST"));
    }

    private static boolean isAllowed(String method, String... headers) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Host", "web:8080");
        for (int i = 0; i < headers.length; i += 2)
            headerMap.put(headers[i], headers[i + 1]);
        return LSFCrossSiteRequestFilter.isAllowed(fake(HttpServletRequest.class, (name, args) -> {
            switch (name) {
                case "getMethod": return method;
                case "getHeader": return headerMap.get(args[0]);
                default: return null;
            }
        }));
    }
}
