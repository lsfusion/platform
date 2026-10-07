package lsfusion.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Tracing;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static lsfusion.tests.TestServer.BASE;
import static lsfusion.tests.TestServer.excerpt;
import static lsfusion.tests.TestServer.read;

/** The measurement spike for browser tests: a test server, the web client's war in a jetty of its own, and a browser
 *  driven by playwright. Every step of every iteration is timed, a failing step is recorded with a screenshot and a
 *  trace and the iteration goes on to the next one, so that one run gives both the times and the failure rate.
 *  -Dui.repeat (10), -Dui.browser (local, or docker: playwright's image), -Dui.channel (chrome: the installed one; empty
 *  for playwright's own chromium), -Dui.headless (true), -Dui.trace (true), -Dui.slowMo (0). The report goes to
 *  target/ui/report.txt. */
public class UiSpikeIT {

    private static final Path UI = BASE.resolve("target/ui");

    private static final String DATABASE = "lsfusion_test_" + ProcessHandle.current().pid();
    private static final String DB_SERVER = System.getProperty("db.server", "localhost");
    private static final String DB_USER = System.getProperty("db.user", "postgres");
    private static final String DB_PASSWORD = System.getProperty("db.password", "");

    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private static final Map<String, List<Long>> times = new LinkedHashMap<>(); // step -> millis of each passed run
    private static final Map<String, List<String>> failures = new LinkedHashMap<>(); // step -> what each failed run said
    private static final List<String> rows = new ArrayList<>(); // iteration,step,millis or failed : every step of every run, for iterations.csv
    private static int iteration; // 0 while the stack starts

    // a step that runs longer than this is caught in the act - see watch
    private static final long SLOW_STEP = Long.getLong("ui.slowStep", 8_000);
    private static final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ui-watchdog");
        thread.setDaemon(true);
        return thread;
    });
    private static final List<String> slow = new ArrayList<>(); // the dumps the watchdog took, for the report
    private static volatile boolean slowIteration; // keep the iteration's trace

    private static Process server, jetty;
    private static int httpPort, webPort;
    private static Playwright playwright;
    private static Browser browser;
    private static String container; // the browser's, with -Dui.browser=docker

    @BeforeClass
    public static void start() throws Exception {
        Files.createDirectories(UI.resolve("failures"));
        Files.createDirectories(UI.resolve("slow"));

        int[] ports = freePorts(5);
        httpPort = ports[0];
        int rmiPort = ports[1];
        webPort = ports[4];

        long started = System.nanoTime();
        Path serverLog = UI.resolve("server.log");
        server = TestServer.start(TestServer.classPath("target/classes"), serverLog,
                "db.name=" + DATABASE, "db.server=" + DB_SERVER, "db.user=" + DB_USER, "db.password=" + DB_PASSWORD,
                "http.port=" + httpPort, "rmi.port=" + rmiPort, "webSocket.port=" + ports[2], "debugger.port=" + ports[3],
                "settings.enableAPI=2", "settings.enableUI=2", // anonymous both ways : neither the runner nor the browser logs in
                "user.language=en", "user.country=US", "user.timezone=UTC"); // the captions and formats follow the server, not the browser
        waitFor(() -> read(serverLog).contains("Server has successfully started"), server, serverLog, "the test server did not start");
        record("server start", started);

        started = System.nanoTime();
        exec("UiSpike.uiSeed[]");
        record("seed", started);

        started = System.nanoTime();
        Path jettyLog = UI.resolve("jetty.log");
        Path jettyTmp = Files.createDirectories(UI.resolve("jetty-tmp")); // its work dirs outlive the forced stop: mvn clean takes them
        jetty = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED", // gwt-rpc serializes LinkedHashMap by reflection
                "-Djava.io.tmpdir=" + jettyTmp,
                // jetty-runner logs nothing without it: the start's phases (annotation scanning, context) for the report
                "-Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog", "-Dorg.eclipse.jetty.LEVEL=INFO",
                "-Duser.language=en", "-Duser.country=US", "-Duser.timezone=UTC",
                "-Dapp.server=localhost", "-Dapp.port=" + rmiPort,
                "-jar", UI.resolve("jetty-runner.jar").toString(), "--port", String.valueOf(webPort), System.getProperty("ui.war"))
                .redirectErrorStream(true).redirectOutput(jettyLog.toFile()).start();
        // jetty itself is up in some 15 s on the Jenkins box, the first answer came 24 s later
        ScheduledFuture<?> watch = watch("000-web client start", 20_000);
        try {
            waitFor(() -> answers("http://localhost:" + webPort + "/"), jetty, jettyLog, "the web client did not start");
        } finally {
            watch.cancel(false);
        }
        record("web client start", started);

        started = System.nanoTime();
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Collections.singletonMap("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        if ("docker".equals(System.getProperty("ui.browser"))) {
            // a box with no browser of its own : playwright's image of the client's version serves one over a websocket,
            // on the host's network, so that it reaches the jetty on localhost
            String version = System.getProperty("ui.playwrightVersion");
            String image = "mcr.microsoft.com/playwright:v" + version;
            docker("pull", "-q", image);
            record("browser image pull", started);

            started = System.nanoTime();
            int browserPort = freePorts(1)[0];
            container = "lsfusion-ui-browser-" + ProcessHandle.current().pid();
            docker("run", "-d", "--rm", "--init", "--ipc=host", "--network", "host", "--name", container,
                    "--user", "pwuser", "--workdir", "/home/pwuser", image,
                    "/bin/sh", "-c", "npx -y playwright@" + version + " run-server --port " + browserPort + " --host 127.0.0.1");
            long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
            while (browser == null)
                try {
                    browser = playwright.chromium().connect("ws://127.0.0.1:" + browserPort + "/", new BrowserType.ConnectOptions().setTimeout(30_000));
                } catch (PlaywrightException e) { // npx is still fetching playwright, or the server is still starting
                    if (System.currentTimeMillis() > deadline)
                        throw e;
                    Thread.sleep(500);
                }
        } else {
            String channel = System.getProperty("ui.channel", "chrome");
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setChannel(channel.isEmpty() ? null : channel)
                    .setHeadless(!"false".equals(System.getProperty("ui.headless")))
                    .setSlowMo(Double.parseDouble(System.getProperty("ui.slowMo", "0"))));
        }
        record("browser launch", started);
    }

    // nothing in the logs tells what a stalled step waits for (on the Jenkins box some loads take 10-30 s instead of 4,
    // and the first answer of the web client 24 s), so the watchdog takes thread dumps of the server and of jetty once
    // the step has run for threshold ms, and every 5 s after while it lasts, up to 4 times ; the iteration keeps its trace
    private static ScheduledFuture<?> watch(String name, long threshold) {
        int[] count = {0};
        return watchdog.scheduleAtFixedRate(() -> {
            if (++count[0] > 4)
                return;
            slowIteration = true;
            String file = "slow/" + name + "-" + count[0] + ".txt";
            StringBuilder dumps = new StringBuilder();
            for (Process process : Arrays.asList(server, jetty))
                if (process != null)
                    try {
                        Process jcmd = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "jcmd").toString(),
                                String.valueOf(process.pid()), "Thread.print").redirectErrorStream(true).start();
                        dumps.append("==== ").append(process == server ? "server" : "jetty").append(" at ").append(java.time.LocalTime.now()).append('\n')
                                .append(new String(jcmd.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).append('\n');
                        jcmd.waitFor();
                    } catch (Exception e) { // a task of the executor that throws is not run again
                        dumps.append("==== ").append(e).append('\n');
                    }
            try {
                Files.write(UI.resolve(file), dumps.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            synchronized (slow) {
                slow.add(file);
            }
        }, threshold, 5_000, TimeUnit.MILLISECONDS);
    }

    private static String docker(String... args) throws Exception {
        List<String> command = new ArrayList<>(Collections.singletonList("docker"));
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0)
            throw new IllegalStateException(String.join(" ", command) + "\n" + output);
        return output;
    }

    @AfterClass
    public static void stop() throws Exception {
        watchdog.shutdownNow();
        if (browser != null)
            browser.close();
        if (playwright != null)
            playwright.close();
        if (container != null)
            docker("rm", "-f", container);
        for (Process process : Arrays.asList(jetty, server))
            if (process != null) {
                process.destroyForcibly();
                process.waitFor();
            }

        String report = report();
        System.out.println(report);
        Files.write(UI.resolve("report.txt"), report.getBytes(StandardCharsets.UTF_8));
        rows.add(0, "iteration,step,ms");
        Files.write(UI.resolve("iterations.csv"), rows);

        if (server == null || Boolean.getBoolean("lsf.keepDb"))
            return;
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + DB_SERVER + "/postgres", DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE + " WITH (FORCE)");
        }
    }

    @Test
    public void measure() throws Exception {
        int repeat = Integer.getInteger("ui.repeat", 10);
        boolean trace = !"false".equals(System.getProperty("ui.trace"));
        for (iteration = 1; iteration <= repeat; iteration++) {
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setLocale("en-US").setTimezoneId("UTC").setViewportSize(1280, 800).setReducedMotion(com.microsoft.playwright.options.ReducedMotion.REDUCE));
            context.setDefaultTimeout(30_000);
            if (trace)
                context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
            Page page = context.newPage();
            List<String> console = new ArrayList<>();
            page.onConsoleMessage(message -> { if ("error".equals(message.type())) console.add(message.text()); });
            page.onPageError(error -> console.add("page error: " + error));

            String failed = null;
            int quantity = 100 + iteration; // under 1000 : the grid shows a thousands separator
            String answer = "answer " + iteration;
            slowIteration = false;
            for (String step : Arrays.asList("load", "items", "edit", "tree", "dialog")) {
                long started = System.nanoTime();
                ScheduledFuture<?> watch = watch(String.format("%03d-%s", iteration, step), SLOW_STEP);
                try {
                    switch (step) {
                        case "load": load(page); break;
                        case "items": openItems(page); break;
                        case "edit": edit(page, quantity); break;
                        case "tree": tree(page); break;
                        case "dialog": dialog(page, answer); break;
                    }
                    record(step, started);
                } catch (Throwable t) {
                    failed = step;
                    rows.add(iteration + "," + step + ",failed");
                    String name = String.format("%03d-%s", iteration, step);
                    try {
                        page.screenshot(new Page.ScreenshotOptions().setPath(UI.resolve("failures/" + name + ".png")).setFullPage(true));
                        Files.write(UI.resolve("failures/" + name + ".html"), page.content().getBytes(StandardCharsets.UTF_8));
                    } catch (Throwable ignored) {
                    }
                    failures.computeIfAbsent(step, s -> new ArrayList<>()).add("#" + iteration + " " + firstLine(t) + (console.isEmpty() ? "" : " | console: " + console));
                    if (trace)
                        context.tracing().stop(new Tracing.StopOptions().setPath(UI.resolve("failures/" + name + ".zip")));
                    break; // the steps after it start from where it should have left the page
                } finally {
                    watch.cancel(false);
                }
            }
            if (trace && failed == null) {
                if (slowIteration) // its network timings, beside the thread dumps the watchdog took
                    context.tracing().stop(new Tracing.StopOptions().setPath(UI.resolve(String.format("slow/%03d.zip", iteration))));
                else
                    context.tracing().stop();
            }
            context.close();
        }
        int failed = failures.values().stream().mapToInt(List::size).sum();
        Assert.assertEquals("failed iterations, see target/ui/report.txt", 0, failed);
    }

    // ---------------------------------------------------------------- the scenarios

    private void load(Page page) {
        page.navigate("http://localhost:" + webPort + "/main");
        page.locator("[lsfusion-container='UiSpike.uiSpike']").waitFor();
    }

    private void openItems(Page page) {
        cell(open(page, "UiSpike.uiItems"), "uiName(i)", "Item 1-1").waitFor();
    }

    private void edit(Page page, int quantity) throws Exception {
        Locator form = form(page, "UiSpike.uiItems");
        Locator cell = row(form, "uiName(i)", "Item 1-1").locator("td[lsfusion-container='PROPERTY(uiQuantity(i))']");
        cell.dblclick();
        Locator editor = cell.locator("input"); // a key typed before the editor is up goes to the grid : ctrl+a selects every row
        editor.fill(String.valueOf(quantity));
        editor.press("Enter");
        assertThat(cell).hasText(Pattern.compile("^" + quantity + "$"));
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
        checkUntil("UiSpike.uiCheckQuantity[STRING[50],INTEGER]", "Item 1-1", String.valueOf(quantity));
        close(form);
    }

    private void tree(Page page) {
        Locator form = open(page, "UiSpike.uiTree");
        // a mouse down on another row's node only selects the row (DataGrid.onCellBefore), a double click opens it
        cell(form, "uiCategoryName(c)", "Category 2").dblclick();
        cell(form, "uiName(i)", "Item 2-3").waitFor();
        close(form);
    }

    private void dialog(Page page, String answer) throws Exception {
        Locator form = open(page, "UiSpike.uiDialog");
        Locator ask = form.locator("[lsfusion-container='PROPERTY(uiAsk())']");
        ask.click();
        // the input opens in place of the button with the response; playwright waits for the glass of a request before
        // a click but not before keys, which the block drops, so they go into the input, which comes after the response
        Locator input = ask.locator("input");
        input.fill(answer);
        input.press("Enter");
        checkUntil("UiSpike.uiCheckAnswer[STRING[100]]", answer);
        close(form);
    }

    // while a form is open, a folder's panel shows only under the mouse or for a moment after a click on the folder -
    // and that moment is turned off under automation (GwtClientUtils.isAutomated), so a scenario closes its form for
    // the next one to reach the navigator
    private static void close(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formClose())']").click();
        form.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
    }

    // a form by its navigator element, unfolding the folder when the element is not on the screen : a folder's panel
    // slides out of the viewport keeping its elements, which playwright still takes for visible. While a request blocks
    // the input (the start runs one right after the navigator is drawn), the client lays a glass over the page under
    // automation, and a click waits for it to go (GBusyDialogDisplayer). Should a click be lost all the same, it is
    // repeated - and every repeat is counted, as a run without it would have failed there
    private static Locator open(Page page, String element) {
        Locator item = page.locator("[lsfusion-container='" + element + "']");
        for (int attempt = 1; !inViewport(item, 1); attempt++) {
            page.locator("[lsfusion-container='UiSpike.uiSpike']").click();
            if (!inViewport(item, 2000)) {
                if (attempt == 5)
                    throw new AssertionError(element + " is not shown after " + attempt + " clicks on its folder");
                record("  (folder click repeated)", System.nanoTime());
            }
        }
        item.click();
        page.mouse().move(640, 400); // the folder's panel stays out over the form while the mouse is on it
        return form(page, element);
    }

    // a timeout of 0 would wait forever : a check of the moment is a timeout of 1 ms
    private static boolean inViewport(Locator locator, double timeout) {
        try {
            assertThat(locator).isInViewport(new com.microsoft.playwright.assertions.LocatorAssertions.IsInViewportOptions().setTimeout(timeout));
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    // a form stays in the page while another one is shown, with the same property names in its cells
    private static Locator form(Page page, String name) {
        return page.locator("[lsfusion-form='" + name + "']");
    }

    private static Locator cell(Locator form, String property, String text) {
        return form.locator(cellSelector(property)).filter(new Locator.FilterOptions().setHasText(exactly(text)));
    }

    private static Locator row(Locator form, String property, String text) {
        // the inner locator of has is matched inside the row, so it starts from the page, not from the form
        return form.locator("tr").filter(new Locator.FilterOptions().setHas(form.page().locator(cellSelector(property)).filter(new Locator.FilterOptions().setHasText(exactly(text)))));
    }

    private static String cellSelector(String property) {
        return "td[lsfusion-container='PROPERTY(" + property + ")']";
    }

    // playwright matches the pattern in javascript, which has no \Q...\E of Pattern.quote
    private static Pattern exactly(String text) {
        return Pattern.compile("^" + text.replaceAll("[\\\\^$.*+?()\\[\\]{}|-]", "\\\\$0") + "$");
    }

    // the server applies what the form sent a moment after the click that sent it, so its check is retried a while
    private static void checkUntil(String action, String... params) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            HttpResponse<String> response = execResponse(action, params);
            if (response.statusCode() == 200)
                return;
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError(action + ": " + response.body());
            Thread.sleep(100);
        }
    }

    // ---------------------------------------------------------------- the stack

    private static void exec(String action, String... params) throws Exception {
        HttpResponse<String> response = execResponse(action, params);
        if (response.statusCode() != 200)
            throw new IllegalStateException(action + ": " + response.body());
    }

    private static HttpResponse<String> execResponse(String action, String... params) throws Exception {
        StringBuilder url = new StringBuilder("http://localhost:" + httpPort + "/exec?action=" + URLEncoder.encode(action, "UTF-8"));
        for (String param : params)
            url.append("&p=").append(URLEncoder.encode(param, "UTF-8"));
        return HTTP.send(HttpRequest.newBuilder(URI.create(url.toString())).timeout(Duration.ofMinutes(1)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static boolean answers(String url) {
        try {
            return HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private interface Check {
        boolean done() throws Exception;
    }

    private static void waitFor(Check check, Process process, Path log, String failure) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
        while (!check.done()) {
            if (!process.isAlive() || System.currentTimeMillis() > deadline)
                throw new IllegalStateException(failure + "\n" + excerpt(read(log)));
            Thread.sleep(250);
        }
    }

    private static int[] freePorts(int count) throws IOException {
        List<ServerSocket> sockets = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++)
                sockets.add(new ServerSocket(0));
            return sockets.stream().mapToInt(ServerSocket::getLocalPort).toArray();
        } finally {
            for (ServerSocket socket : sockets)
                socket.close();
        }
    }

    // ---------------------------------------------------------------- the report

    private static synchronized void record(String step, long startedNanos) {
        long millis = (System.nanoTime() - startedNanos) / 1_000_000;
        times.computeIfAbsent(step, s -> new ArrayList<>()).add(millis);
        rows.add(iteration + "," + step.trim() + "," + millis);
    }

    private static String firstLine(Throwable t) {
        String message = String.valueOf(t.getMessage());
        int end = message.indexOf('\n');
        return t.getClass().getSimpleName() + ": " + (end < 0 ? message : message.substring(0, end));
    }

    private static String report() {
        StringBuilder report = new StringBuilder(String.format("%-18s %5s %6s %8s %8s %8s %8s%n", "step", "runs", "failed", "min", "median", "p90", "max"));
        List<String> steps = new ArrayList<>(times.keySet());
        for (String step : failures.keySet())
            if (!steps.contains(step))
                steps.add(step);
        for (String step : steps) {
            List<Long> passed = new ArrayList<>(times.getOrDefault(step, Collections.emptyList()));
            Collections.sort(passed);
            int failed = failures.getOrDefault(step, Collections.emptyList()).size();
            report.append(passed.isEmpty() ? String.format("%-18s %5d %6d%n", step, failed, failed)
                    : String.format("%-18s %5d %6d %8d %8d %8d %8d%n", step, passed.size() + failed, failed,
                    passed.get(0), passed.get(passed.size() / 2), passed.get((int) Math.ceil(passed.size() * 0.9) - 1), passed.get(passed.size() - 1)));
        }
        for (Map.Entry<String, List<String>> entry : failures.entrySet())
            for (String failure : entry.getValue())
                report.append(entry.getKey()).append(' ').append(failure).append('\n');
        // each iteration on a line of its own, so that a slow one shows which step it was slow in
        Map<String, StringBuilder> byIteration = new LinkedHashMap<>();
        for (String row : rows) {
            String[] cells = row.split(",");
            if (!cells[0].equals("0"))
                byIteration.computeIfAbsent(cells[0], i -> new StringBuilder("#" + i)).append(' ').append(cells[1]).append('=').append(cells[2]);
        }
        for (StringBuilder line : byIteration.values())
            report.append(line).append('\n');
        synchronized (slow) {
            for (String file : slow)
                report.append("slow: target/ui/").append(file).append('\n');
        }
        return report.toString();
    }
}
