package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// The API requests of a browser logged in to the web client run as the user from the web client's own pages only: not
// from a page of another site, nor when the user opens the address. "Another site" is a page server of the test's own:
// on localhost it is another origin of the same site (another port), on 127.0.0.1 another site. A server on the
// loopback, not a page Playwright answers itself: the browser takes such a page for one outside the machine and refuses
// its requests to localhost (Local Network Access). Each request records the user it ran as under a tag of its own
// (UiCsrf.lsf), or nothing when it did not run
public class UiCsrfIT extends UiTest {

    private static HttpServer pages;

    @BeforeClass
    public static void startPages() throws Exception {
        pages = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        pages.createContext("/", exchange -> {
            byte[] page = "<!doctype html><title>another site</title><body></body>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
            exchange.close();
        });
        pages.start();
    }

    @AfterClass
    public static void stopPages() {
        pages.stop(0);
    }

    private static String pages(String host) {
        return "http://" + host + ":" + pages.getAddress().getPort() + "/";
    }

    private String login() {
        return login(webClient());
    }

    // logs the page's browser context in to the web client at the address as admin: the session's cookie is the
    // context's from now on. Returns the response's Set-Cookie of the session
    private String login(String web) {
        return sessionCookie(page.context().request().post(web + "/login_check", RequestOptions.create()
                .setForm(FormData.create().set("username", "admin").set("password", "")).setMaxRedirects(0)));
    }

    private static String sessionCookie(APIResponse response) {
        return response.headersArray().stream().filter(header -> header.name.equalsIgnoreCase("Set-Cookie") && header.value.startsWith("JSESSIONID="))
                .map(header -> header.value).findFirst().orElseThrow(() -> new AssertionError("no session cookie in the answer: " + response.headersArray()));
    }

    private static String tag(String name) {
        return name + System.nanoTime();
    }

    private static String record(String tag) {
        return "/exec?action=UiCsrf.uiCsrfRecord&p=" + tag;
    }

    private static String script(String tag) {
        return "run() { uiCsrfRecord('" + tag + "'); }";
    }

    private static String evalLink(String tag) {
        return "/eval?script=" + URLEncoder.encode(script(tag), StandardCharsets.UTF_8);
    }

    // the Authorization header of admin's Basic login with the password
    private static String basic(String password) {
        return "Basic " + Base64.getEncoder().encodeToString(("admin:" + password).getBytes(StandardCharsets.UTF_8));
    }

    // a page at the first address opens the web client's address as a page, and the web client answers it - an action
    // that returns nothing answers with a file the browser downloads. Returns the answer. A tab of its own, as the page
    // an answer loads may still be loading when the next address is opened; the address is opened after the evaluate
    // returns, which a fast navigation would cut off
    private Response follow(String from, String url) {
        Page tab = page.context().newPage();
        tab.navigate(from);
        return tab.waitForResponse(response -> response.url().equals(url),
                () -> tab.evaluate("url => { setTimeout(() => { location.href = url; }); }", url));
    }

    // a page of another site opens the web client's address as a page
    private Response follow(String url) {
        return follow(pages("127.0.0.1"), url);
    }

    // the user opens the web client's address in the browser - typed, a bookmark, a link another application hands to
    // it -, a navigation no page starts. Returns the answer
    private Response open(String url) {
        Page tab = page.context().newPage();
        return tab.waitForResponse(response -> response.url().equals(url), () -> {
            try {
                tab.navigate(url);
            } catch (PlaywrightException e) { // an action that ran answers with a file, which the tab does not show
                if (!e.getMessage().contains("Download is starting"))
                    throw e;
            }
        });
    }

    // the request did not run (checked first, as a request that ran tells more than the answer's status), and the web
    // client refused it. Returns the answer
    private static Response assertRefused(String tag, Response response) throws Exception {
        ranAs(tag, "none");
        assertEquals(403, response.status());
        return response;
    }

    private static void ranAs(String tag, String user) throws Exception {
        check("UiCsrf.uiCsrfCheck", tag, user);
    }

    // the requests a page of the web client sends - a custom view's, WebMCP's - run as the user
    @Test
    public void ownPageRunsAsUser() throws Exception {
        login();
        page.navigate(webClient() + "/main");
        String exec = tag("exec"), eval = tag("eval"), mcp = tag("mcp");
        page.evaluate("([exec, eval, mcp]) => Promise.all(["
                + " fetch(exec, { method: 'POST' }),"
                + " fetch('/eval', { method: 'POST', body: new URLSearchParams({ script: eval }) }),"
                + " fetch('/mcp', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id: 1,"
                + "   method: 'tools/call', params: { name: 'lsfusion_eval', arguments: { script: mcp } } }) })])",
                Arrays.asList(record(exec), script(eval), script(mcp)));
        ranAs(exec, "admin");
        ranAs(eval, "admin");
        ranAs(mcp, "admin");
    }

    // a page of another origin of the same site: neither its script's requests nor its image run as the user
    @Test
    public void otherOriginRefused() throws Exception {
        login();
        page.navigate(pages("localhost"));
        String eval = tag("eval"), mcp = tag("mcp"), image = tag("image");
        page.evaluate("([web, eval, mcp, image]) => Promise.all(["
                + " fetch(web + '/eval', { method: 'POST', mode: 'no-cors', credentials: 'include', headers: { 'Content-Type': 'text/plain' }, body: eval }),"
                + " fetch(web + '/mcp', { method: 'POST', mode: 'no-cors', credentials: 'include', headers: { 'Content-Type': 'text/plain' },"
                + "   body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'tools/call', params: { name: 'lsfusion_eval', arguments: { script: mcp } } }) }),"
                + " new Promise(done => { const img = new Image(); img.onload = img.onerror = done; img.src = web + image; })])",
                Arrays.asList(webClient(), script(eval), script(mcp), record(image)));
        ranAs(eval, "none");
        ranAs(mcp, "none");
        ranAs(image, "none");
    }

    // a link of another site opened as a page runs as the user neither an action nor a script
    @Test
    public void otherSiteLinkOpensNoAction() throws Exception {
        login();
        String action = tag("link"), eval = tag("evalLink"), execEval = tag("execEvalLink");
        assertRefused(action, follow(webClient() + record(action)));
        assertRefused(eval, follow(webClient() + evalLink(eval)));
        assertRefused(execEval, follow(webClient() + "/exec" + evalLink(execEval)));
    }

    // an address the user opens in the browser runs as the user neither an action nor a script: the browser tells it
    // from no other request a page does not start
    @Test
    public void userOpenedLinkOpensNoAction() throws Exception {
        login();
        String action = tag("openedLink"), eval = tag("openedEvalLink");
        assertEquals("none", assertRefused(action, open(webClient() + record(action))).request().allHeaders().get("sec-fetch-site"));
        assertRefused(eval, open(webClient() + evalLink(eval)));
    }

    // over plain http to a host other than localhost - the machine's name - a browser describes a GET neither by
    // Sec-Fetch-Site nor by an Origin: a link of a page of the web client runs as the user, by the Referer it comes
    // with, and neither a link of another site nor an address the user opens does
    @Test
    public void plainHttpLinkRunsByReferer() throws Exception {
        String web = webClient().replace("localhost", InetAddress.getLocalHost().getHostName().toLowerCase(Locale.ROOT)); // as the browser has it
        login(web);
        String own = tag("plainHttpOwnLink"), other = tag("plainHttpLink"), opened = tag("plainHttpOpened");
        Map<String, String> sent = follow(web + "/main", web + record(own)).request().allHeaders();
        ranAs(own, "admin");
        assertEquals(Arrays.asList(null, null, true), Arrays.asList(sent.get("sec-fetch-site"), sent.get("origin"), sent.get("referer").startsWith(web + "/")));
        assertRefused(other, follow(web + record(other)));
        assertRefused(opened, open(web + record(opened)));
    }

    // a request no browser describes - no Sec-Fetch-Site, no Origin -, as a client other than a browser sends it: a GET
    // runs as the user with a Referer of the web client and not without it; a POST runs without it as well
    @Test
    public void undescribedRequestRunsByReferer() throws Exception {
        login();
        String action = tag("plainAction"), evalLink = tag("plainEvalLink"), refused = tag("plainRefused"), evalPost = tag("plainEvalPost");
        RequestOptions own = RequestOptions.create().setHeader("Referer", webClient() + "/main");
        page.context().request().get(webClient() + record(action), own);
        page.context().request().get(webClient() + evalLink(evalLink), own);
        int refusedStatus = page.context().request().get(webClient() + record(refused)).status();
        page.context().request().post(webClient() + "/eval", RequestOptions.create().setForm(FormData.create().set("script", script(evalPost))));
        ranAs(action, "admin");
        ranAs(evalLink, "admin");
        ranAs(refused, "none");
        assertEquals(403, refusedStatus);
        ranAs(evalPost, "admin");
    }

    // a client with a token of its own that keeps the session cookie /exec and /eval answer with: its request is not
    // checked and runs, a GET of a script with no Referer included
    @Test
    public void tokenRequestRunsAsToken() throws Exception {
        login();
        page.navigate(webClient() + "/main");
        String token = (String) page.evaluate("() => fetch('/exec?action=Authentication.getAuthToken', { method: 'POST' }).then(response => response.text())");
        String eval = tag("tokenEvalLink");
        page.context().request().get(webClient() + evalLink(eval), RequestOptions.create().setHeader("Authorization", "Bearer " + token));
        ranAs(eval, "admin");
    }

    // a client with a Basic login of its own that keeps the session cookie as well: its GET of a script with no Referer
    // runs as the login, its password checked though the login is the session's user's
    @Test
    public void basicRequestRunsAsLogin() throws Exception {
        page.navigate(pages("127.0.0.1")); // a wrong password drops the session's login, under no page of the web client
        login();
        String right = tag("rightPassword"), wrong = tag("wrongPassword");
        page.context().request().get(webClient() + evalLink(right), RequestOptions.create().setHeader("Authorization", basic("")));
        assertEquals(401, page.context().request().get(webClient() + evalLink(wrong), RequestOptions.create().setHeader("Authorization", basic("wrong"))).status());
        ranAs(right, "admin");
        ranAs(wrong, "none");
    }

    // the session's cookie is SameSite=Lax, the one a session is created with and the one the login changes it to: a
    // form another site posts gets no cookie, even right after the login, so the request runs as the anonymous user the
    // test server lets in
    @Test
    public void otherSitePostGetsNoCookie() throws Exception {
        page.navigate(pages("127.0.0.1")); // no page of the web client is left open without its session
        page.context().clearCookies();
        String created = sessionCookie(page.context().request().get(webClient() + "/main", RequestOptions.create().setMaxRedirects(0)));
        assertTrue("a new session's cookie: " + created, created.toLowerCase().contains("samesite=lax"));
        String cookie = login();
        assertTrue("the session's cookie: " + cookie, cookie.toLowerCase().contains("samesite=lax"));
        String post = tag("post");
        page.waitForResponse(response -> response.url().contains(post), () -> page.evaluate("url => { const form = document.createElement('form');"
                + " form.method = 'POST'; form.action = url; document.body.appendChild(form); form.submit(); }", webClient() + record(post)));
        ranAs(post, "anonymous");
    }
}
