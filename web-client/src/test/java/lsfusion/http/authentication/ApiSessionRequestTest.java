package lsfusion.http.authentication;

import lsfusion.interop.connection.AuthenticationToken;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

// The web server's security as security-config.xml declares it: an API request on a logged-in session of the web
// client runs as the session's user from a page of the web client only (LSFCrossSiteRequestFilter)
public class ApiSessionRequestTest extends ApiSecurityTest {

    @Test
    public void ownPageRequestRuns() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals("t", call("same-origin", response));
        assertEquals(200, response.getStatus());
    }

    // typed, a bookmark, a link another application hands to the browser
    @Test
    public void userOpenedRequestRefused() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(call("none", response));
        assertEquals(403, response.getStatus());
    }

    // a line break in the path keeps a request to /eval an API request
    @Test
    public void lineBreakPathRequestRefused() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(call("cross-site", "/eval/%0a/eval", "/eval", "/\n/eval", response));
        assertEquals(403, response.getStatus());
    }

    private static String call(String site, MockHttpServletResponse response) throws Exception {
        return call(site, "/exec", "/exec", null, response);
    }

    // the token of the session's user the API handler runs with, null when the request does not reach it
    private static String call(String site, String uri, String servletPath, String pathInfo, MockHttpServletResponse response) throws Exception {
        MockHttpSession session = new MockHttpSession();
        LSFAuthenticationToken user = new LSFAuthenticationToken("user", "", new AuthenticationToken("t"), Locale.ROOT);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(user));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setServletPath(servletPath);
        request.setPathInfo(pathInfo);
        request.setSession(session);
        request.addHeader("Sec-Fetch-Site", site);
        String[] token = new String[1];
        security.doFilter(request, response, (filtered, filteredResponse) -> token[0] = LSFAuthenticationToken.getAppServerToken().string);
        return token[0];
    }
}
