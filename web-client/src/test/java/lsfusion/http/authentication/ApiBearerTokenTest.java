package lsfusion.http.authentication;

import lsfusion.interop.connection.AuthenticationToken;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

// The web server's security as security-config.xml declares it: an API request with an "Authorization: Bearer" token
// runs as that token, its security context neither read from the request's HTTP session nor saved there - the session
// keeps the user it has, none or a logged-in one
public class ApiBearerTokenTest extends ApiSecurityTest {

    private static final String FILE = "/file/static/a.js"; // needs a login

    @Test
    public void tokenDoesNotLogTheSessionIn() throws Exception {
        MockHttpSession session = new MockHttpSession();

        assertEquals("x", call("/exec", session, "Bearer x"));
        assertNull(call(FILE, session, null));
        assertNull(call(FILE, session, "Bearer x"));
    }

    @Test
    public void tokenKeepsTheSessionsUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        LSFAuthenticationToken user = new LSFAuthenticationToken("user", "", new AuthenticationToken("t"), Locale.ROOT);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(user));

        assertEquals("x", call("/exec", session, "Bearer x"));
        assertEquals("t", call("/exec", session, null));
        assertEquals("t", call(FILE, session, null));
    }

    // the token the request reaches its handler with, null if it does not reach it; the handler takes the session, as
    // ExternalLogicsAndSessionRequestHandler does
    private static String call(String path, MockHttpSession session, String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setSession(session);
        request.addHeader("Sec-Fetch-Site", "same-origin"); // the web client's own page
        if (authorization != null)
            request.addHeader("Authorization", authorization);
        String[] token = new String[1];
        security.doFilter(request, new MockHttpServletResponse(), (filtered, response) -> {
            ((HttpServletRequest) filtered).getSession();
            token[0] = LSFAuthenticationToken.getAppServerToken().string;
        });
        return token[0];
    }
}
