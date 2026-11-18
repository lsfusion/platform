package lsfusion.http.controller;

import lsfusion.http.authentication.LSFLoginUrlAuthenticationEntryPoint;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.http.Fakes.fake;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

// A request that fails for want of a login is opened again after the login, on the session it logs in: not a request
// LSFCrossSiteRequestFilter refuses on a session. The request is a fake: a GET opened as a page
public class ExternalRequestHandlerTest {

    @Test
    public void otherSiteRequestNotOpenedAfterLogin() throws Exception {
        assertNull(openedAfterLogin("cross-site"));
    }

    // over plain http to a host other than localhost a browser sends no Sec-Fetch-Site, and a link of another site
    // comes without the site cookie
    @Test
    public void plainHttpLinkNotOpenedAfterLogin() throws Exception {
        assertNull(openedAfterLogin(null));
    }

    @Test
    public void ownRequestOpenedAfterLogin() throws Exception {
        assertEquals("/exec?action=a", openedAfterLogin("same-origin"));
    }

    // what the login opens after it, once the request with the Sec-Fetch-Site, if any, failed for want of it
    private static String openedAfterLogin(String site) throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        HttpSession session = fake(HttpSession.class, (method, args) -> {
            switch (method) {
                case "setAttribute": attributes.put((String) args[0], args[1]); return null;
                case "getAttribute": return attributes.get(args[0]);
                default: return null;
            }
        });
        Map<String, String> headers = new HashMap<>();
        headers.put("Sec-Fetch-Site", site);
        HttpServletRequest request = fake(HttpServletRequest.class, (method, args) -> {
            switch (method) {
                case "getSession": return session;
                case "getHeader": return headers.get(args[0]);
                case "getMethod": return "GET";
                case "getServletPath": return "/exec";
                case "getQueryString": return "action=a";
                case "getContextPath": return "";
                default: return null;
            }
        });
        ExternalRequestHandler.processFailedAuthentication(request, fake(HttpServletResponse.class, (method, args) -> null), new RuntimeException(), null);
        return LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);
    }
}
