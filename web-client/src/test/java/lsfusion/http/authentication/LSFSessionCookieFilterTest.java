package lsfusion.http.authentication;

import org.junit.Test;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static lsfusion.http.Fakes.fake;
import static org.junit.Assert.assertEquals;

// LSFSessionCookieFilter gives the session's Set-Cookie SameSite=Lax unless the cookie has a SameSite attribute, with a
// space after the ";" or without: the word in another attribute (a Path=/samesite) is none. Another cookie of the
// response stays. The container is a fake: the request's getSession(true) adds the session's cookie to the response,
// as a container does
public class LSFSessionCookieFilterTest {

    private static final String OTHER_COOKIE = "LSFUSION_LOCALE=en; Path=/\n";

    @Test
    public void sameSiteInPathIsNoSameSite() throws Exception {
        assertEquals(OTHER_COOKIE + "JSESSIONID=1A2B; Path=/samesite; HttpOnly; SameSite=Lax", createSession("JSESSIONID=1A2B; Path=/samesite; HttpOnly"));
    }

    @Test
    public void sameSiteKept() throws Exception {
        assertEquals(OTHER_COOKIE + "JSESSIONID=1A2B; Path=/; HttpOnly; SameSite=Strict", createSession("JSESSIONID=1A2B; Path=/; HttpOnly; SameSite=Strict"));
        assertEquals(OTHER_COOKIE + "JSESSIONID=1A2B;Path=/;SameSite=None", createSession("JSESSIONID=1A2B;Path=/;SameSite=None"));
    }

    // the response's Set-Cookie once a request through the filter creates the session the container sets the cookie of
    private static String createSession(String cookie) throws Exception {
        List<String> headers = new ArrayList<>(Collections.singletonList(OTHER_COOKIE.trim()));
        HttpServletResponse response = fake(HttpServletResponse.class, (method, args) -> {
            switch (method) {
                case "getHeaders": return new ArrayList<>(headers);
                case "setHeader": headers.clear(); headers.add((String) args[1]); return null;
                case "addHeader": headers.add((String) args[1]); return null;
                default: return null;
            }
        });
        HttpSession session = fake(HttpSession.class, (method, args) -> method.equals("getId") ? "1A2B" : null);
        HttpServletRequest request = fake(HttpServletRequest.class, (method, args) -> {
            switch (method) {
                case "getSession": headers.add(cookie); return session;
                case "getDispatcherType": return DispatcherType.REQUEST;
                default: return null;
            }
        });
        FilterChain chain = (filtered, filteredResponse) -> ((HttpServletRequest) filtered).getSession(true);
        new LSFSessionCookieFilter().doFilter(request, response, chain);
        return String.join("\n", headers);
    }
}
