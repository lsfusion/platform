package lsfusion.http.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.ServerMessages;
import lsfusion.http.authentication.LSFClientRegistrationRepository;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.logics.ServerSettings;
import org.json.JSONTokener;
import org.junit.Test;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Locale;

import static lsfusion.http.Fakes.fake;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

// What the controller makes of a request's values for a page that writes them as they are: the notification page's
// script gets the id as a number and the query as a string literal, the script of the page of an unavailable server
// its address as a string literal - a string literal holds the value as it is, with no "<" in it -, the login page the
// links to the desktop client with the address in them escaped and as it is. The requests are fakes, with a query a
// browser would not send as it is, and so is the application server
public class MainControllerTest {

    private static final String QUERY = "x=\"</script><!--\\";

    @Test
    public void notificationPageGetsLiterals() {
        ModelMap model = new ModelMap();
        controller(unavailableServer()).pushNotification(model, request("1", "notification_id=1&" + QUERY));
        assertEquals(1, model.get("id"));
        assertScriptString(QUERY, model.get("query"));

        model = new ModelMap();
        controller(unavailableServer()).pushNotification(model, request("1</script>", QUERY));
        assertNull(model.get("id"));
    }

    @Test
    public void unavailableServerPageGetsALiteral() {
        ModelMap model = new ModelMap();
        assertEquals("app-not-available", controller(unavailableServer()).processMain(model, request(null, QUERY), null));
        assertScriptString("/main?" + QUERY, model.get("redirectURL"));
    }

    // the application's template takes the address after the text of the link
    @Test
    public void loginPageLinksTheDesktopClientAtTheAddress() {
        HttpServletRequest request = request(null, "x=\"&{run.desktop.client}$5");
        ModelMap model = new ModelMap();
        assertEquals("login", controller(server("<a href=\"{runDesktopQuery}&p=1\">{run.desktop.client}</a>")).processLogin(model, request));
        assertEquals("<a href=\"/exec?x=&quot;&amp;{run.desktop.client}$5&amp;action=Security.generateJnlp&p=1\">" + ServerMessages.getString(request, "run.desktop.client") + "</a>",
                model.get("jnlpUrls"));
    }

    private static void assertScriptString(String value, Object literal) {
        assertFalse(String.valueOf(literal), String.valueOf(literal).contains("<"));
        JSONTokener tokener = new JSONTokener((String) literal);
        assertEquals(value, tokener.nextValue());
        assertEquals(0, tokener.nextClean()); // the literal is the whole of it
    }

    private static MainController controller(LogicsProvider logicsProvider) {
        return new MainController(logicsProvider, null, new LSFClientRegistrationRepository(logicsProvider), null);
    }

    // an application server that gives no settings and fails a request
    private static LogicsProvider unavailableServer() {
        return fake(LogicsProvider.class, (method, args) -> {
            if (method.equals("runRequest"))
                throw new IllegalStateException("not available");
            return null;
        });
    }

    // an application server of the web client's version with the links to the desktop client of the template, and with
    // no OAuth clients
    private static LogicsProvider server(String jnlpUrls) {
        ServerSettings settings = new ServerSettings(null, null, null, null, null, BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion(),
                false, 0, false, jnlpUrls, false, null, null, null, false);
        return fake(LogicsProvider.class, (method, args) -> {
            switch (method) {
                case "getServerSettings": return settings;
                case "runRequest": return Collections.emptyList();
                default: return null;
            }
        });
    }

    private static HttpServletRequest request(String notificationId, String query) {
        HttpSession session = fake(HttpSession.class, (method, args) -> null);
        return fake(HttpServletRequest.class, (method, args) -> {
            switch (method) {
                case "getParameter": return args[0].equals("notification_id") ? notificationId : null;
                case "getQueryString": return query;
                case "getContextPath": return "";
                case "getSession": return session;
                case "getLocale": return Locale.ENGLISH;
                default: return null;
            }
        });
    }
}
