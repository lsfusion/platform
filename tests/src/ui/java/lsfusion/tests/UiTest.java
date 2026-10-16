package lsfusion.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static lsfusion.tests.TestServer.BASE;

/** The base of the browser tests: a test server, the web client's war in a jetty of its own and a browser driven by
 *  playwright. The stack is one for all the classes of the run: the first test starts it, the jvm's exit stops it.
 *  Each test gets a browser context of its own with /main open in it. A failed test leaves a screenshot, the page and
 *  a trace in target/ui/failures/<Class>.<method>.*, and its message lists the errors the page logged; one listed in
 *  quarantine.txt as <Class>.<method> is reported as skipped. -Dui.browser is a playwright channel (chrome, msedge) or
 *  docker - playwright's image, for a box with no browser; -Dui.headless=false shows the browser; -Dui.watchdog=<ms>
 *  takes thread dumps of the server and of jetty into target/ui/slow every that many ms a test runs. */
public abstract class UiTest {

    private static final Path UI = BASE.resolve("target/ui");
    private static final String DATABASE = "lsfusion_test_ui_" + ProcessHandle.current().pid(); // LsfServerIT, in the same jvm, takes lsfusion_test_<pid>
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private static final long WATCHDOG = Long.getLong("ui.watchdog", 0);
    private static final ScheduledExecutorService DUMPS = Executors.newSingleThreadScheduledExecutor();
    // every load logs these two: chrome has no push api in an incognito context, which a test's is
    private static final Pattern PUSH_API = Pattern.compile("Push API|Registration failed - permission denied");

    private static Process server, jetty;
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

    // the server, jetty and the browser start side by side: the web client looks the server up on a request, not at
    // its own start
    private static void start() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }));
        long started = System.nanoTime();

        int[] ports = TestServer.freePorts(6); // the server's four, jetty's and docker's browser's: taken at once, none comes twice
        httpPort = ports[0];
        webPort = ports[4];
        Path serverLog = UI.resolve("server.log");
        server = TestServer.start(DATABASE, ports, serverLog,
                "settings.enableAPI=2", "settings.enableUI=2", // anonymous both ways : neither /exec nor the browser logs in
                "user.language=en", "user.country=US", "user.timezone=UTC"); // the captions and formats follow the server, not the browser

        Path jettyLog = UI.resolve("jetty.log");
        jetty = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED", // gwt-rpc serializes LinkedHashMap by reflection
                "-Djava.io.tmpdir=" + Files.createDirectories(UI.resolve("jetty-tmp")), // its work dirs outlive the forced stop: mvn clean takes them
                "-Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog", "-Dorg.eclipse.jetty.LEVEL=INFO", // jetty-runner logs nothing otherwise
                "-Duser.language=en", "-Duser.country=US", "-Duser.timezone=UTC",
                "-Dapp.server=localhost", "-Dapp.port=" + ports[1],
                "-jar", UI.resolve("jetty-runner.jar").toString(), "--port", String.valueOf(webPort), System.getProperty("ui.war"))
                .redirectErrorStream(true).redirectOutput(jettyLog.toFile()).start();

        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Collections.singletonMap("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        String channel = System.getProperty("ui.browser", "chrome");
        if (channel.equals("docker")) {
            // playwright's image of the client's version serves a browser over a websocket, on the host's network, so
            // that it reaches the jetty on localhost
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
        }, jetty, jettyLog, "the web client did not start");
        System.out.printf(Locale.ROOT, "lsFusion UI test stack up in: browser %.1f s, server %.1f s, web client %.1f s; database %s, logs in %s%n",
                browserUp, serverUp, (System.nanoTime() - started) / 1e9, DATABASE, UI);
    }

    // a throw ends the hook, so what nothing else would stop goes first: the processes and the container outlive the jvm,
    // playwright's driver does not
    private static void stop() throws Exception {
        if (jetty != null)
            jetty.destroyForcibly().waitFor();
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
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setLocale("en-US").setTimezoneId("UTC").setViewportSize(1280, 800).setReducedMotion(ReducedMotion.REDUCE));
            context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            page = context.newPage();
            List<String> console = new ArrayList<>();
            page.onConsoleMessage(message -> {
                if (message.type().equals("error") && !PUSH_API.matcher(message.text()).find())
                    console.add(message.text());
            });
            page.onPageError(error -> {
                if (!PUSH_API.matcher(error).find())
                    console.add("page error: " + error);
            });
            ScheduledFuture<?> watch = WATCHDOG > 0 ? DUMPS.scheduleAtFixedRate(dumps(name), WATCHDOG, WATCHDOG, TimeUnit.MILLISECONDS) : null;
            try {
                page.navigate("http://localhost:" + webPort + "/main");
                page.locator("[lsfusion-container='System.root']").waitFor(); // the navigator's top window
                test.evaluate();
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

    // nothing in the logs tells what a stalled test waits for, the threads of the server and of jetty do
    private static Runnable dumps(String name) {
        int[] count = {0};
        return () -> {
            count[0]++;
            try {
                Path slow = Files.createDirectories(UI.resolve("slow"));
                for (Process process : Arrays.asList(server, jetty))
                    new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "jcmd").toString(), String.valueOf(process.pid()), "Thread.print")
                            .redirectErrorStream(true).redirectOutput(slow.resolve(name + "-" + count[0] + (process == server ? "-server" : "-jetty") + ".txt").toFile())
                            .start().waitFor();
            } catch (Exception e) { // thrown, it would cancel the dumps that follow
                e.printStackTrace();
            }
        };
    }

    // ---------------------------------------------------------------- helpers

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
