package lsfusion.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static lsfusion.tests.TestServer.BASE;

/** The base of the browser tests: a test server, the web client's war in a web container of its own and a browser
 *  driven by playwright. The stack is one for all the classes of the run: the first test starts it, the jvm's exit
 *  stops it. Each test gets a browser context of its own with /main open in it. A failed test leaves a screenshot, the
 *  page and a trace in target/ui/failures/<Class>.<method>.*, and its message lists the errors the page logged; one
 *  listed in quarantine.txt as <Class>.<method> is reported as skipped. -Dui.container is the web container, jetty,
 *  which the client is developed with, or tomcat, which it is shipped in; -Dui.browser is a playwright channel (chrome,
 *  msedge) or docker - playwright's image, for a box with no browser; -Dui.headless=false shows the browser;
 *  -Dui.watchdog=<ms> takes thread dumps of the server and of the web container into target/ui/slow every that many ms
 *  a test runs. */
public abstract class UiTest {

    private static final Path UI = BASE.resolve("target/ui");
    private static final String DATABASE = "lsfusion_test_ui_" + ProcessHandle.current().pid(); // LsfServerIT, in the same jvm, takes lsfusion_test_<pid>
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private static final long WATCHDOG = Long.getLong("ui.watchdog", 0);
    private static final String WEB_CONTAINER = System.getProperty("ui.container", "jetty");
    private static final ScheduledExecutorService DUMPS = Executors.newSingleThreadScheduledExecutor();

    private static Process server, web; // the test server and the web container
    private static int httpPort, webPort;
    private static Playwright playwright;
    private static Browser browser;
    private static String container; // the browser's, with -Dui.browser=docker
    private static Throwable startFailure; // the classes after the first one fail with it rather than start another stack

    protected Page page;

    @BeforeClass
    public static void startStack() throws Throwable {
        if (startFailure != null)
            throw startFailure;
        if (browser == null)
            try {
                start();
            } catch (Throwable t) {
                throw startFailure = t;
            }
    }

    // the server, the web container and the browser start side by side: the web client looks the server up on a
    // request, not at its own start
    private static void start() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }));
        long started = System.nanoTime();

        int[] ports = TestServer.freePorts(6); // the server's four, the web container's and docker's browser's: taken at once, none comes twice
        httpPort = ports[0];
        webPort = ports[4];
        Path serverLog = UI.resolve("server.log");
        server = TestServer.start(DATABASE, ports, serverLog,
                "settings.enableAPI=2", "settings.enableUI=2", // anonymous both ways : neither /exec nor the browser logs in
                "user.language=en", "user.country=US", "user.timezone=UTC"); // the captions and formats follow the server, not the browser

        Path webLog = UI.resolve(WEB_CONTAINER + ".log");
        List<String> command = new ArrayList<>(Arrays.asList(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED", // gwt-rpc serializes LinkedHashMap by reflection
                "-Djava.io.tmpdir=" + Files.createDirectories(UI.resolve(WEB_CONTAINER + "-tmp")), // its work dirs outlive the forced stop: mvn clean takes them
                "-Duser.language=en", "-Duser.country=US", "-Duser.timezone=UTC",
                "-Dapp.server=localhost", "-Dapp.port=" + ports[1]));
        if (WEB_CONTAINER.equals("tomcat")) {
            // the distribution as the client's docker image runs it, the war as ROOT, only on our port and in an appBase
            // without the distribution's manager, docs and examples
            Path home = Paths.get(System.getProperty("ui.tomcat"));
            Path webapps = Files.createDirectories(UI.resolve("tomcat-webapps"));
            Files.copy(Paths.get(System.getProperty("ui.war")), webapps.resolve("ROOT.war"), StandardCopyOption.REPLACE_EXISTING);
            Path config = UI.resolve("tomcat-server.xml");
            Files.write(config, new String(Files.readAllBytes(home.resolve("conf/server.xml")), StandardCharsets.UTF_8)
                    .replace("port=\"8005\"", "port=\"-1\"").replace("port=\"8080\"", "port=\"" + webPort + "\"")
                    .replace("appBase=\"webapps\"", "appBase=\"" + webapps + "\"").getBytes(StandardCharsets.UTF_8));
            command.addAll(Arrays.asList("-Dcatalina.home=" + home, "-Dcatalina.base=" + home,
                    "-cp", home.resolve("bin/bootstrap.jar") + File.pathSeparator + home.resolve("bin/tomcat-juli.jar"),
                    "org.apache.catalina.startup.Bootstrap", "-config", config.toString(), "start"));
        } else
            command.addAll(Arrays.asList("-Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog", "-Dorg.eclipse.jetty.LEVEL=INFO", // jetty-runner logs nothing otherwise
                    "-jar", UI.resolve("jetty-runner.jar").toString(), "--port", String.valueOf(webPort), System.getProperty("ui.war")));
        web = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(webLog.toFile()).start();

        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Collections.singletonMap("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        String channel = System.getProperty("ui.browser", "chrome");
        if (channel.equals("docker")) {
            // playwright's image of the client's version serves a browser over a websocket, on the host's network, so
            // that it reaches the web client on localhost
            String version = System.getProperty("ui.playwrightVersion");
            int port = ports[5];
            container = "lsfusion-ui-browser-" + ProcessHandle.current().pid();
            Process run = new ProcessBuilder("docker", "run", "-d", "--rm", "--init", "--ipc=host", "--network", "host", "--name", container,
                    "--user", "pwuser", "--workdir", "/home/pwuser", "mcr.microsoft.com/playwright:v" + version,
                    "/bin/sh", "-c", "npx -y playwright@" + version + " run-server --port " + port + " --host 127.0.0.1")
                    .redirectErrorStream(true).start();
            String output = new String(run.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (run.waitFor() != 0)
                throw new IllegalStateException("docker run failed\n" + output);
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
            while (browser == null)
                try {
                    browser = playwright.chromium().connect("ws://127.0.0.1:" + port + "/", new BrowserType.ConnectOptions().setTimeout(30_000));
                } catch (PlaywrightException e) { // npx is still fetching playwright, or its server is still starting
                    if (System.currentTimeMillis() > deadline) { // what npx or the server said: the hook removes the container only at the jvm's exit
                        Process logs = new ProcessBuilder("docker", "logs", container).redirectErrorStream(true).start();
                        throw new IllegalStateException("no browser in " + container + "\n" + new String(logs.getInputStream().readAllBytes(), StandardCharsets.UTF_8), e);
                    }
                    Thread.sleep(500);
                }
        } else
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel(channel)
                    .setHeadless(!"false".equals(System.getProperty("ui.headless"))));
        double browserUp = (System.nanoTime() - started) / 1e9;

        TestServer.waitStarted(server, serverLog);
        double serverUp = (System.nanoTime() - started) / 1e9;
        TestServer.waitFor(() -> {
            try {
                return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + webPort + "/")).timeout(Duration.ofSeconds(5)).build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode() < 500;
            } catch (IOException e) { // not listening yet
                return false;
            }
        }, web, webLog, "the web client did not start");
        System.out.printf(Locale.ROOT, "lsFusion UI test stack up in: browser %.1f s, server %.1f s, web client in %s %.1f s; database %s, logs in %s%n",
                browserUp, serverUp, WEB_CONTAINER, (System.nanoTime() - started) / 1e9, DATABASE, UI);
    }

    // a throw ends the hook, so what nothing else would stop goes first: the processes and the container outlive the jvm,
    // playwright's driver does not
    private static void stop() throws Exception {
        if (web != null)
            web.destroyForcibly().waitFor();
        if (container != null)
            new ProcessBuilder("docker", "rm", "-f", container).start().waitFor();
        if (server != null)
            TestServer.stop(server, DATABASE);
        if (playwright != null)
            playwright.close(); // and the browser with it
    }

    @Rule
    public final TestRule browserContext = (test, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            String name = description.getTestClass().getSimpleName() + "." + description.getMethodName();
            // no service worker, which the client registers at every load for push notifications no test uses:
            // playwright 1.63's driver keeps a closed context's page alive by its session (microsoft/playwright#43009),
            // so the driver grew by every test's requests and responses - with the ~40 MB gwt-dev bundle tracing reads
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setServiceWorkers(ServiceWorkerPolicy.BLOCK)
                    .setLocale("en-US").setTimezoneId("UTC").setViewportSize(1280, 800).setReducedMotion(ReducedMotion.REDUCE));
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            page = context.newPage();
            List<String> console = new ArrayList<>();
            List<String> pageErrors = new ArrayList<>();
            page.onConsoleMessage(message -> {
                if (message.type().equals("error"))
                    console.add(message.text());
            });
            page.onPageError(error -> {
                pageErrors.add(error);
                console.add("uncaught page error: " + error);
            });
            ScheduledFuture<?> watch = WATCHDOG > 0 ? DUMPS.scheduleAtFixedRate(dumps(name), WATCHDOG, WATCHDOG, TimeUnit.MILLISECONDS) : null;
            try {
                page.navigate("http://localhost:" + webPort + "/main");
                page.locator("[lsfusion-container='System.root']").waitFor(); // the navigator's top window
                test.evaluate();
                page.evaluate("() => 0"); // playwright hands the page's events to the handlers only during a call into it
                if (!pageErrors.isEmpty())
                    throw new AssertionError("uncaught page errors:\n" + String.join("\n", pageErrors));
                context.tracing().stop();
            } catch (Throwable t) {
                Path failure = Files.createDirectories(UI.resolve("failures")).resolve(name);
                try {
                    page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(failure + ".png")).setFullPage(true));
                    Files.write(Paths.get(failure + ".html"), page.content().getBytes(StandardCharsets.UTF_8));
                    context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(failure + ".zip")));
                } catch (Throwable e) { // a crashed page has none of them to give
                    t.addSuppressed(e);
                }
                String message = t + (console.isEmpty() ? "" : "\nerrors the page logged:\n" + String.join("\n", console));
                if (TestServer.quarantine().contains(name))
                    throw new AssumptionViolatedException(message, t); // still run, so it is known when it starts passing again
                throw new AssertionError(message, t);
            } finally {
                if (watch != null)
                    watch.cancel(false);
                context.close();
            }
        }
    };

    // nothing in the logs tells what a stalled test waits for, the threads of the server and of the web container do
    private static Runnable dumps(String name) {
        int[] count = {0};
        return () -> {
            count[0]++;
            try {
                Path slow = Files.createDirectories(UI.resolve("slow"));
                for (Process process : Arrays.asList(server, web))
                    new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "jcmd").toString(), String.valueOf(process.pid()), "Thread.print")
                            .redirectErrorStream(true).redirectOutput(slow.resolve(name + "-" + count[0] + (process == server ? "-server" : "-" + WEB_CONTAINER) + ".txt").toFile())
                            .start().waitFor();
            } catch (Exception e) { // thrown, it would cancel the dumps that follow
                e.printStackTrace();
            }
        };
    }

    // ---------------------------------------------------------------- helpers

    protected void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // the client drops keys typed while a request blocks the input
    protected void press(Locator editor, String key) {
        idle();
        editor.press(key);
    }

    protected void press(String key) {
        idle();
        page.keyboard().press(key);
    }

    protected void pressAndWait(String key) {
        press(key);
        idle();
    }

    protected void key(Locator cell, String key) {
        click(cell);
        press(key);
    }

    protected void focusAndPress(Locator element, String key) {
        element.click();
        press(key);
    }

    // a row change is sent later and has no glass; its answer must be applied before the next click
    protected void click(Locator cell) {
        answered(Boolean.TRUE.equals(cell.evaluate("cell => !!cell.closest('tr') && !cell.closest('tr').classList.contains('table-active')")) ? 1 : 0,
                cell::click);
    }

    protected void select(Locator cell) {
        page.waitForResponse(response -> carries(response.request().postData(), "ChangeGroupObject"), cell::click).finished();
    }

    // a dialog's OK follows the row request in server order, so sending it is enough there
    protected void selectSent(Locator cell) {
        rowChangeSent(cell::click);
    }

    protected void rowChangeSent(Runnable step) {
        page.waitForRequest(request -> carries(request.postData(), "ChangeGroupObject"), step);
    }

    // loadend precedes the next task, where the GWT handlers have applied the answer
    private static final String REQUESTS = "(() => { if (window.uiTestRequests) return; const requests = window.uiTestRequests = {pending: 0, changes: 0};"
            + " const send = XMLHttpRequest.prototype.send; XMLHttpRequest.prototype.send = function (body) { requests.pending++;"
            + " if (String(body).includes('action.form.ChangeGroupObject/')) requests.changes++;"
            + " this.addEventListener('loadend', () => setTimeout(() => requests.pending--)); return send.apply(this, arguments); }; })()";
    private Page trackedPage;

    protected void trackRequests() {
        if (trackedPage != page) {
            page.addInitScript(REQUESTS);
            trackedPage = page;
        }
        page.evaluate(REQUESTS);
    }

    protected void answered(int changes, Runnable step) {
        trackRequests();
        int before = ((Number) page.evaluate("() => window.uiTestRequests.changes")).intValue();
        step.run();
        page.waitForFunction("before => window.uiTestRequests.changes >= before && window.uiTestRequests.pending === 0", before + changes);
    }

    protected void settled() {
        answered(0, () -> { });
    }

    protected void popup(String text) {
        Locator popup = page.locator("[data-tippy-root]");
        assertThat(popup).hasText(text);
        page.waitForFunction("() => !!document.activeElement && !!document.activeElement.closest('[data-tippy-root]')");
        page.evaluate("() => document.activeElement.blur()");
        assertThat(popup).hasCount(0);
    }

    // a dialog the platform shows, saying the text - a message, an error -, and it closed by its Close button
    protected Locator dialog(String text) {
        return page.locator(".modal-content").filter(new Locator.FilterOptions().setHasText(text));
    }
    protected void closeDialog(String text) {
        dialog(text).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close").setExact(true)).click();
        assertThat(dialog(text)).hasCount(0);
    }

    protected static Locator button(Locator scope, String title) {
        return scope.getByTitle(title, new Locator.GetByTitleOptions().setExact(true));
    }

    protected static Locator action(Locator form, String action) {
        return propertyButton(form, action + "()");
    }

    protected static Locator propertyButton(Locator form, String property) {
        return form.locator("button[lsfusion-container='PROPERTY(" + property + ")']");
    }

    protected static Locator text(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    protected List<String> requests() {
        return requests(request -> true);
    }

    protected List<String> requests(String action) {
        return requests(request -> carries(request.postData(), action));
    }

    protected List<String> requests(Predicate<Request> filter) {
        List<String> bodies = new CopyOnWriteArrayList<>();
        page.onRequest(request -> {
            String body = request.postData();
            if (body != null && filter.test(request))
                bodies.add(body);
        });
        return bodies;
    }

    // a report of an error may name an action too, so a form request needs its full GWT-RPC signature
    protected static String signature(String kind) {
        return "action.form." + kind + "/";
    }

    protected static long count(List<String> requests, String kind) {
        return requests.stream().filter(body -> body.contains(signature(kind))).count();
    }

    protected static boolean carries(String body, String action) {
        return body != null && body.contains("." + action + "/");
    }

    protected static int countActions(List<String> requests, String action) {
        return (int) requests.stream().filter(body -> carries(body, action)).count();
    }

    private final List<Route> held = new ArrayList<>();

    protected void hold() {
        page.route("**/main/dispatch", held::add);
    }

    protected void release() {
        page.unroute("**/main/dispatch");
        for (Route route : held)
            route.resume();
        held.clear();
    }

    protected List<String> consoleErrors() {
        List<String> errors = new ArrayList<>();
        page.onConsoleMessage(message -> {
            if (message.type().equals("error"))
                errors.add(message.text());
        });
        return errors;
    }

    protected View view(String state, String bindings) {
        return new View("const view = " + state + ", data = view && view.data, controller = view && view.controller;" + bindings,
                "!!data", "the view is drawn with ", "(" + state + ") && (" + state + ").data");
    }

    protected class View {
        private final String scope, ready, description, diagnostic;

        protected View(String scope, String ready, String description, String diagnostic) {
            this.scope = scope;
            this.ready = ready;
            this.description = description;
            this.diagnostic = diagnostic;
        }

        public void waitFor(String condition) {
            try {
                page.waitForFunction("() => { " + scope + " try { return " + ready + " && !!(" + condition + "); } catch (e) { return false; } }",
                        null, new Page.WaitForFunctionOptions().setTimeout(10_000));
            } catch (PlaywrightException e) {
                throw new AssertionError("never " + condition + (description.isEmpty() ? "" : " - " + description
                        + page.evaluate("() => JSON.stringify(" + diagnostic + ")")), e);
            }
        }

        public View drawn() {
            waitFor("true");
            return this;
        }

        public Object value(String expression) {
            return page.evaluate("() => { " + scope + " return " + expression + "; }");
        }

        public String eval(String expression) {
            return (String) page.evaluate("async () => { " + scope + " return String(await (" + expression + ")); }");
        }

        // a returned Promise would make evaluate wait for the answer, which hold() may keep back
        public void call(String call) {
            page.evaluate("() => { " + scope + " " + call + "; }");
        }

        public String answer(String call) {
            return (String) page.evaluate("async () => { " + scope + " const answer = " + call + ";"
                    + " if (!answer || typeof answer.then !== 'function') return 'not a Promise: ' + answer; return String(await answer); }");
        }

        public String refusal(String call) {
            return (String) page.evaluate("async () => { " + scope + " try { await (" + call + "); return 'not refused'; } catch (e) { return e.message; } }");
        }

        public String rejection(String call) {
            return (String) page.evaluate("async () => { " + scope + " const answer = " + call + ";"
                    + " if (!answer || typeof answer.then !== 'function') return 'not a Promise: ' + answer;"
                    + " try { await answer; return 'not rejected'; } catch (e) { return e.message; } }");
        }

        public String syncRefusal(String call) {
            return syncRefusal(call, null);
        }

        public String syncRefusal(String call, String success) {
            return (String) page.evaluate("success => { " + scope + " try { " + call + "; return success; } catch (e) { return e.message; } }", success);
        }
    }

    // a form by its navigator element, named after the form, unfolding the folder when the element is not on the screen:
    // a folder's panel slides out of the viewport keeping its elements, which playwright still takes for visible. While
    // a form is open, the panel does not come out on a click under automation (GwtClientUtils.isAutomated), so a test
    // closes its form before it opens another one
    protected Locator open(String folder, String element) {
        Locator item = page.locator("[lsfusion-container='" + element + "']");
        if (!inViewport(item)) {
            page.locator("[lsfusion-container='" + folder + "']").click();
            assertThat(item).isInViewport();
        }
        item.click();
        page.mouse().move(640, 400); // the folder's panel stays out over the form while the mouse is on it
        return form(element);
    }

    // a timeout of 0 would wait forever : a check of the moment is a timeout of 1 ms
    private static boolean inViewport(Locator locator) {
        try {
            assertThat(locator).isInViewport(new LocatorAssertions.IsInViewportOptions().setTimeout(1));
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    protected void close(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formClose())']").click();
        form.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // a form stays in the page while another one is shown, with the same property names in its cells
    protected Locator form(String name) {
        return page.locator("[lsfusion-form='" + name + "']");
    }

    protected Locator cell(Locator form, String property, String text) {
        return form.locator(cellSelector(property)).filter(new Locator.FilterOptions().setHasText(exactly(text)));
    }

    protected Locator row(Locator form, String property, String text) {
        // the inner locator of has is matched inside the row, so it starts from the page, not from the form
        return form.locator("tr").filter(new Locator.FilterOptions().setHas(page.locator(cellSelector(property)).filter(new Locator.FilterOptions().setHasText(exactly(text)))));
    }

    private static String cellSelector(String property) {
        return "td[lsfusion-container='PROPERTY(" + property + ")']";
    }

    // playwright matches the pattern in javascript, which has no \Q...\E of Pattern.quote
    private static Pattern exactly(String text) {
        return Pattern.compile("^" + text.replaceAll("[\\\\^$.*+?()\\[\\]{}|-]", "\\\\$0") + "$");
    }

    // a double click opens the editor - a mouse down on another row only selects the row - and the text is filled into
    // its input: a key typed before the editor is up goes to the grid, where ctrl+a selects every row
    protected void edit(Locator cell, String text) {
        cell.dblclick();
        Locator editor = cell.locator("input");
        editor.fill(text);
        editor.press("Enter");
    }

    protected static void exec(String action, String... params) throws Exception {
        exec(0, action, params);
    }

    // the server applies what the browser sent a moment after the click that sent it, so a check is retried for 10 s
    protected static void check(String action, String... params) throws Exception {
        exec(10_000, action, params);
    }

    private static void exec(long retry, String action, String... params) throws Exception {
        StringBuilder url = new StringBuilder("http://localhost:" + httpPort + "/exec?action=" + URLEncoder.encode(action, StandardCharsets.UTF_8));
        for (String param : params)
            url.append("&p=").append(URLEncoder.encode(param, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString())).timeout(Duration.ofMinutes(1)).POST(HttpRequest.BodyPublishers.noBody()).build();
        long deadline = System.currentTimeMillis() + retry;
        while (true) {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200)
                return;
            if (System.currentTimeMillis() >= deadline)
                throw new AssertionError(action + ": " + response.body());
            Thread.sleep(100);
        }
    }
}
