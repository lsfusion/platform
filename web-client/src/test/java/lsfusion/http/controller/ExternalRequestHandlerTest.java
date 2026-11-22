package lsfusion.http.controller;

import lsfusion.http.authentication.LSFLoginUrlAuthenticationEntryPoint;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.apache.hc.core5.http.ContentType;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.http.Fakes.fake;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// A request that fails for want of a login is opened again after the login, on the session it logs in: not a request
// LSFCrossSiteRequestFilter refuses on a session. An error of a request is answered as plain text, with its message as
// it is. The requests are fakes: a GET opened as a page, and so is the application server: it fails the request with
// the exception given
public class ExternalRequestHandlerTest {

    @Test
    public void otherSiteRequestNotOpenedAfterLogin() throws Exception {
        assertNull(openedAfterLogin("cross-site"));
    }

    // the address bar, a bookmark, a link another application hands to the browser
    @Test
    public void userOpenedRequestNotOpenedAfterLogin() throws Exception {
        assertNull(openedAfterLogin("none"));
    }

    // over plain http to a host other than localhost a browser sends no Sec-Fetch-Site, and a link of another site
    // comes without the Referer of the web client
    @Test
    public void plainHttpLinkNotOpenedAfterLogin() throws Exception {
        assertNull(openedAfterLogin(null));
    }

    @Test
    public void ownRequestOpenedAfterLogin() throws Exception {
        assertEquals("/exec?action=a", openedAfterLogin("same-origin"));
    }

    @Test
    public void errorAnsweredAsText() throws Exception {
        assertAnsweredAsText(new RemoteInternalException("Action <b>a</b> was not found", null, null, 404), 404);
    }

    @Test
    public void refusedRequestAnsweredAsText() throws Exception {
        assertAnsweredAsText(new AuthenticationException("User <b>a</b> is not authenticated"), 401);
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

    private static void assertAnsweredAsText(Exception error, int status) throws Exception {
        String[] contentType = new String[1];
        int[] answeredStatus = new int[1];
        StringWriter body = new StringWriter();
        HttpServletResponse response = fake(HttpServletResponse.class, (method, args) -> {
            switch (method) {
                case "setContentType": contentType[0] = (String) args[0]; return null;
                case "setStatus": answeredStatus[0] = (Integer) args[0]; return null;
                case "getWriter": return new PrintWriter(body, true);
                default: return null;
            }
        });
        LogicsProvider logicsProvider = fake(LogicsProvider.class, (method, args) -> {
            if (method.equals("runRequest")) {
                try {
                    return ((LogicsRunnable<?>) args[args.length - 1]).run(null, true);
                } catch (RemoteException e) {
                    throw new AssertionError(e);
                }
            }
            return null;
        });
        // the answer to a refused call names the address of the server's OAuth metadata
        HttpServletRequest request = fake(HttpServletRequest.class, (method, args) -> {
            switch (method) {
                case "getScheme": return "http";
                case "getServerName": return "localhost";
                case "getServerPort": return 80;
                default: return null;
            }
        });

        new ExternalRequestHandler(logicsProvider) {
            @Override
            protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
                throw error;
            }
        }.handleRequest(request, response);

        assertEquals(status, answeredStatus[0]);
        assertEquals(contentType[0], ContentType.TEXT_PLAIN.getMimeType(), contentType[0] == null ? null : ContentType.parse(contentType[0]).getMimeType());
        assertTrue(body.toString(), body.toString().contains(error.getMessage()));
    }
}
