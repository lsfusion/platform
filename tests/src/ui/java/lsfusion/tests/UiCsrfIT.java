package lsfusion.tests;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.SameSiteAttribute;
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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// The API requests of a browser logged in to the web client run as the user from the web client's own pages, not from
// a page of another site. "Another site" is a page server of the test's own: on
// localhost it is another origin of the same site (another port), on 127.0.0.1 another site. A server on the loopback,
// not a page Playwright answers itself: the browser takes such a page for one outside the machine and refuses its
// requests to localhost (Local Network Access). Each request records the user it ran as under a tag of its own
// (UiCsrf.lsf), or nothing when it did not run
public class UiCsrfIT extends UiTest {

    private static final String SITE_COOKIE = "LSFUSION_SAME_SITE"; // LSFCrossSiteRequestFilter's

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

    // a client that keeps the session's cookie alone: every answer sets the site cookie again
    private void dropSiteCookie() {
        page.context().clearCookies(new BrowserContext.ClearCookiesOptions().setName(SITE_COOKIE));
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

    // a page of another site opens the web client's address as a page, and the web client answers it - an action that
    // returns nothing answers with a file the browser downloads. Returns the answer. A tab of its own, as the page an
    // answer loads may still be loading when the next address is opened; the address is opened after the evaluate
    // returns, which a fast navigation would cut off
    private Response follow(String url) {
        Page tab = page.context().newPage();
        tab.navigate(pages("127.0.0.1"));
        return tab.waitForResponse(response -> response.request().isNavigationRequest(),
                () -> tab.evaluate("url => { setTimeout(() => { location.href = url; }); }", url));
    }

    // a page of another site opens the address as a page: the request did not run (checked first, as a request that ran
    // tells more than the answer's status), and the web client refused it. Returns the answer
    private Response assertRefused(String tag, String url) throws Exception {
        Response response = follow(url);
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
        assertRefused(action, webClient() + record(action));
        assertRefused(eval, webClient() + evalLink(eval));
        assertRefused(execEval, webClient() + "/exec" + evalLink(execEval));
    }

    // over plain http to a host other than localhost - the machine's name - a browser describes a GET neither by
    // Sec-Fetch-Site nor by an Origin, and a link of another site comes with the session's cookie, but not with the
    // site cookie the login's answer set, SameSite=Strict
    @Test
    public void plainHttpLinkOpensNoAction() throws Exception {
        String web = webClient().replace("localhost", InetAddress.getLocalHost().getHostName());
        login(web);
        Object siteCookie = page.context().cookies(web).stream().filter(cookie -> cookie.name.equals(SITE_COOKIE)).map(cookie -> cookie.sameSite).findFirst().orElse(null);
        String link = tag("plainHttpLink");
        Map<String, String> sent = assertRefused(link, web + record(link)).request().allHeaders();
        assertEquals(Arrays.asList(SameSiteAttribute.STRICT, null, null, true, false), Arrays.asList(siteCookie, sent.get("sec-fetch-site"),
                sent.get("origin"), sent.get("cookie").contains("JSESSIONID="), sent.get("cookie").contains(SITE_COOKIE)));
    }

    // a request no browser describes - no Sec-Fetch-Site, no Origin -, as a client other than a browser sends it, or a
    // browser over plain http to a host other than localhost: with the site cookie, which a client with a cookie jar
    // keeps and a browser sends with a request the user opens or a page of the web client sends, it runs as the user, a
    // GET of a script included; a POST runs without it as well
    @Test
    public void undescribedRequestRunsWithSiteCookie() throws Exception {
        page.navigate(pages("127.0.0.1")); // no page of the web client sets the site cookie again
        login();
        String action = tag("plainAction"), evalLink = tag("plainEvalLink"), evalPost = tag("plainEvalPost");
        page.context().request().get(webClient() + record(action));
        page.context().request().get(webClient() + evalLink(evalLink));
        dropSiteCookie();
        page.context().request().post(webClient() + "/eval", RequestOptions.create().setForm(FormData.create().set("script", script(evalPost))));
        ranAs(action, "admin");
        ranAs(evalLink, "admin");
        ranAs(evalPost, "admin");
    }

    // a client with a token of its own that keeps the session cookie /exec and /eval answer with, but not the site
    // cookie: its request is not checked and runs, a GET of a script included
    @Test
    public void tokenRequestRunsAsToken() throws Exception {
        login();
        page.navigate(webClient() + "/main");
        String token = (String) page.evaluate("() => fetch('/exec?action=Authentication.getAuthToken', { method: 'POST' }).then(response => response.text())");
        page.navigate(pages("127.0.0.1")); // no page of the web client sets the site cookie again
        String eval = tag("tokenEvalLink");
        dropSiteCookie();
        page.context().request().get(webClient() + evalLink(eval), RequestOptions.create().setHeader("Authorization", "Bearer " + token));
        ranAs(eval, "admin");
    }

    // a client with a Basic login of its own that keeps the session cookie as well, but not the site cookie: its GET of
    // a script runs as the login, its password checked though the login is the session's user's
    @Test
    public void basicRequestRunsAsLogin() throws Exception {
        page.navigate(pages("127.0.0.1")); // a wrong password drops the session's login, under no page of the web client
        login();
        String right = tag("rightPassword"), wrong = tag("wrongPassword");
        dropSiteCookie();
        page.context().request().get(webClient() + evalLink(right), RequestOptions.create().setHeader("Authorization", basic("")));
        dropSiteCookie();
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
