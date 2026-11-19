package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// WebMCP: the page registers the tools of the application's MCP server with the browser's agent. A tool is the server's
// - its description, what it is called with and what it answers -, and the page substitutes only the run of
// lsfusion_eval where the user works: in the navigator, or in the form the call's `form` says. And a tool of its own,
// lsfusion_client, which calls a method of a form's controller, or of the navigator's. The agent is a stand-in here: a
// document.modelContext that keeps the tools registered with it, and refuses one as a browser does - a name taken or
// not a name, no description, no run. The page registers its tools as it loads, so the stand-in is put there before,
// and the page each test gets is loaded again
public class UiWebMCPIT extends UiTest {

    // the stand-in's registerTool as the browsers that return no promise from it yet have it (Chrome 149-150): a
    // refusal is thrown. With `async` before it a refusal is a rejected promise
    private static final String REGISTER_TOOL = "tool => {"
            + "   if (!/^[A-Za-z0-9_.-]{1,128}$/.test(tool.name) || tool.name in window.uiWebMCPTools)"
            + "     throw new DOMException('the name is taken, or is not one: ' + tool.name, 'InvalidStateError');"
            + "   if (typeof tool.description !== 'string' || !tool.description || typeof tool.execute !== 'function')"
            + "     throw new TypeError('not a tool: ' + tool.name);"
            + "   window.uiWebMCPTools[tool.name] = tool;"
            + " }";

    private void loadWithAgent() {
        loadWithAgent("async " + REGISTER_TOOL);
    }

    // `taken` is a tool another script of the page registered before the page's own
    private void loadWithAgent(String registerTool, String... taken) {
        page.addInitScript("if (window === window.top) {"
                + " window.uiWebMCPTools = Object.create(null);"
                + " Object.defineProperty(document, 'modelContext', { configurable: true, value: { registerTool: " + registerTool + " } });"
                + " for (const name of " + Arrays.toString(Arrays.stream(taken).map(name -> "'" + name + "'").toArray()) + ")"
                + "   document.modelContext.registerTool({ name, description: 'taken', execute: () => 'taken' });"
                // the server's own answer to a request, for what the page gives to be compared with
                + " window.uiWebMCPServer = (method, params) => fetch('mcp', { method: 'POST', body: JSON.stringify({ jsonrpc: '2.0', id: 1, method, params }) })"
                + "   .then(response => response.json()).then(rpc => rpc.result);"
                // a tool's answer as text: its data - in one shape whatever order its fields came in, the results that
                // have names in the order of the names -, or the text of its failure
                + " window.uiWebMCPShape = value => Array.isArray(value) ? value.map(window.uiWebMCPShape) : value && typeof value === 'object'"
                + "   ? Object.fromEntries(Object.keys(value).sort().map(key => [key, window.uiWebMCPShape(value[key])])) : value;"
                + " window.uiWebMCPText = answer => { if (answer && answer.isError) return answer.content[0].text;"
                + "   if (answer && answer.results) answer.results.sort((a, b) => (a.name || '') < (b.name || '') ? -1 : 1);"
                + "   return JSON.stringify(window.uiWebMCPShape(answer)); };"
                + "}");
        page.reload();
        page.locator("[lsfusion-container='System.root']").waitFor();
        page.waitForFunction("() => window.uiWebMCPTools.lsfusion_eval"); // one request brings them all
    }

    // what a tool answers the agent: its data as JSON, or the text of its failure
    private String call(String tool, String arguments) {
        return call(tool, arguments, "answer => window.uiWebMCPText(answer)");
    }

    // ... and `pick`, a JS function, of that data
    private String call(String tool, String arguments, String pick) {
        return String.valueOf(page.evaluate("([tool, args]) => Promise.resolve(window.uiWebMCPTools[tool].execute(JSON.parse(args)))"
                + ".then(answer => answer && answer.isError ? answer.content[0].text : (" + pick + ")(answer))", Arrays.asList(tool, arguments)));
    }

    // what the script returned, each result as its value. An answer does not say where the script ran: a form's
    // session or a navigator's is seen by what the script reads, and a run on the server by the request the page sends
    private static final String VALUES = "answer => answer.results.map(entry => entry.value).join(' | ')";

    // the page's requests to the server for a tool's run, from now on
    private List<String> toolCalls() {
        return requests(request -> request.postData().contains("\"method\":\"tools/call\""));
    }

    // the tools are the server's, their descriptions too - the one of the tool the page runs itself among them -, but
    // the page's own
    @Test
    public void thePageRegistersTheToolsOfTheServer() {
        loadWithAgent();
        String described = "tools => tools.map(tool => tool.name + ': ' + tool.description + JSON.stringify(window.uiWebMCPShape(tool.inputSchema))).sort().join('\\n')";
        Object listed = page.evaluate("() => window.uiWebMCPServer('tools/list').then(list => (" + described + ")(list.tools))");
        assertTrue("the server lists lsfusion_eval among its tools: " + listed, String.valueOf(listed).contains("lsfusion_eval: "));
        assertTrue("... and not the page's own: " + listed, !String.valueOf(listed).contains(CLIENT_TOOL + ": "));
        assertEquals(listed, page.evaluate("() => (" + described + ")(Object.keys(window.uiWebMCPTools).filter(name => name !== '" + CLIENT_TOOL + "').map(name => window.uiWebMCPTools[name]))"));
        assertEquals("form,params,script", page.evaluate("() => Object.keys(window.uiWebMCPTools.lsfusion_eval.inputSchema.properties).sort().join()"));
    }

    // the browser refuses a tool whose name another script of the page has taken: the page's other tools are registered
    @Test
    public void aToolTheBrowserRefusesLeavesTheOthers() {
        aToolTheBrowserRefusesLeavesTheOthers("async " + REGISTER_TOOL);
    }

    @Test
    public void aToolTheBrowserRefusesByAThrowLeavesTheOthers() {
        aToolTheBrowserRefusesLeavesTheOthers(REGISTER_TOOL);
    }

    private void aToolTheBrowserRefusesLeavesTheOthers(String registerTool) {
        loadWithAgent(registerTool, "lsfusion_files_list");
        assertEquals("\"taken\"", call("lsfusion_files_list", "{}", "answer => JSON.stringify(answer)"));
        assertEquals("2", call("lsfusion_eval", "{\"script\":\"run() { RETURN 1 + 1; }\"}", VALUES));
        String files = call("lsfusion_files_search", "{\"regex\":\"MODULE UiWebMCP;\",\"pathPattern\":\"**/UiWebMCP.lsf\"}");
        assertTrue(files, files.contains("/ui/UiWebMCP.lsf\""));
    }

    // the page runs the script itself, where the user works: with no `form`, in the navigator
    @Test
    public void evalRunsInTheNavigator() {
        loadWithAgent();
        List<String> onServer = toolCalls();
        assertEquals("42", call("lsfusion_eval", "{\"script\":\"run(INTEGER x) { RETURN x * 2; }\",\"params\":[21]}", VALUES));
        assertEquals("42", call("lsfusion_eval", "{\"script\":\"run(INTEGER x) { RETURN x * 2; }\",\"params\":[\"21\"]}", VALUES));
        assertEquals("", call("lsfusion_eval", "{\"script\":\"run() { }\"}", VALUES));
        // the answer is the tool's, as the server gives it: the page adds nothing to it
        assertEquals("results,status", call("lsfusion_eval", "{\"script\":\"run() { }\"}", "answer => Object.keys(answer).sort().join()"));
        assertTrue(onServer.toString(), onServer.isEmpty()); // the server ran none of them
    }

    // with `form`, in a form - `{}` is the form the user is in: its session holds what the script changed and did not
    // apply, and the form shows it. With no `form` the script is the navigator's, whatever form the user is in: a
    // session of its own, which reads what is saved, and a message it shows is shown to the user
    @Test
    public void evalRunsInTheFormTheUserIsIn() throws Exception {
        String saved = "saved " + System.nanoTime(), draft = "draft " + System.nanoTime();
        exec("UiWebMCP.uiWebMCPSeed", saved);
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCP");
        Locator note = form.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input");
        assertThat(note).hasValue(saved);

        String read = "run() { RETURN UiWebMCP.uiWebMCPNote(); }";
        List<String> onServer = toolCalls();
        assertEquals("",
                call("lsfusion_eval", "{\"script\":\"run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; }\",\"params\":[\"" + draft + "\"],\"form\":{}}", VALUES));
        assertThat(note).hasValue(draft);
        assertEquals(draft, call("lsfusion_eval", "{\"script\":\"" + read + "\",\"form\":{}}", VALUES));
        assertEquals(saved, call("lsfusion_eval", "{\"script\":\"" + read + "\"}", VALUES));
        // ... a message of the script is the user's to see, so the answer has none
        assertEquals(saved + " | none", call("lsfusion_eval", "{\"script\":\"run() { MESSAGE 'said' NOWAIT; RETURN UiWebMCP.uiWebMCPNote(); }\"}",
                "answer => (" + VALUES + ")(answer) + ' | ' + (answer.messages ? answer.messages.join() : 'none')"));
        assertTrue(onServer.toString(), onServer.isEmpty()); // the server ran none of them
    }

    // the exec tool is the eval tool for an action that is in the logic, and so on the page: the page runs the action
    // where the user works, in the navigator or with `form` in a form
    @Test
    public void execRunsAnActionOfTheLogicWhereTheUserWorks() throws Exception {
        String saved = "saved " + System.nanoTime(), draft = "draft " + System.nanoTime();
        exec("UiWebMCP.uiWebMCPSeed", saved);
        loadWithAgent();
        List<String> onServer = toolCalls();
        assertEquals("42", call("lsfusion_exec", "{\"action\":\"UiWebMCP.uiWebMCPDouble\",\"params\":[21]}", VALUES));

        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCP");
        assertThat(form.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input")).hasValue(saved);
        call("lsfusion_eval", "{\"script\":\"run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; }\",\"params\":[\"" + draft + "\"],\"form\":{}}");
        String read = "\"action\":\"UiWebMCP.uiWebMCPReadNote\"";
        assertEquals(draft, call("lsfusion_exec", "{" + read + ",\"form\":{}}", VALUES)); // in the form's session
        assertEquals(draft, call("lsfusion_exec", "{" + read + ",\"form\":{\"name\":\"UiWebMCP.uiWebMCP\"}}", VALUES));
        assertEquals(saved, call("lsfusion_exec", "{" + read + "}", VALUES)); // in the navigator's
        assertTrue(onServer.toString(), onServer.isEmpty()); // the page ran them itself

        // what is no call of the tool the server says; an action that is not there is refused where it was to run
        String noAction = call("lsfusion_exec", "{\"params\":[]}");
        assertTrue(noAction, noAction.startsWith("lsfusion_exec error: ") && noAction.contains("'action' is required"));
        assertEquals(onServer.toString(), 1, onServer.size());
        for (String where : new String[] {",\"form\":{}", ""}) {
            String unknown = call("lsfusion_exec", "{\"action\":\"UiWebMCP.uiWebMCPNoSuchAction\"" + where + "}");
            assertTrue(unknown, unknown.startsWith("lsfusion_exec error: ") && unknown.contains("uiWebMCPNoSuchAction"));
        }
        assertEquals(onServer.toString(), 1, onServer.size());
    }

    // the write keeps the form's unsaved changes unsaved; formRecreate opens the form with the code
    @Test
    public void customizeFormUserWritesTheCodeFormRecreateShowsIt() throws Exception {
        String saved = "saved " + System.nanoTime(), draft = "draft " + System.nanoTime();
        exec("UiWebMCP.uiWebMCPSeed", saved);
        exec("UiWebMCP.uiWebMCPDropCustomization");
        try {
            loadWithAgent();
            Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCP");
            assertThat(form.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input")).hasValue(saved); // the form is open
            call("lsfusion_eval", "{\"script\":\"run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; }\",\"params\":[\"" + draft + "\"],\"form\":{}}");

            assertEquals("", call("lsfusion_exec", "{\"action\":\"SystemEvents.customizeFormUser\",\"form\":{},"
                    + "\"params\":[\"UiWebMCP.uiWebMCP\",\"EXTEND FORM uiWebMCP PROPERTIES() uiWebMCPExtra\"]}", VALUES));
            String read = "\"script\":\"run() { RETURN UiWebMCP.uiWebMCPNote(); }\"";
            assertEquals(saved, call("lsfusion_eval", "{" + read + "}", VALUES)); // the user's change is not saved
            assertEquals(draft, call("lsfusion_eval", "{" + read + ",\"form\":{}}", VALUES)); // and is still the form's
            assertThat(form.locator("[lsfusion-container='PROPERTY(uiWebMCPExtra())']")).hasCount(0);

            assertEquals("", call("lsfusion_exec", "{\"action\":\"SystemEvents.formRecreate\",\"form\":{}}", VALUES));
            // the recreated form is the copy the code makes, under a name of its own
            assertThat(page.locator("[lsfusion-container='PROPERTY(uiWebMCPExtra())']")).isVisible();
        } finally {
            exec("UiWebMCP.uiWebMCPDropCustomization");
        }
    }

    @Test
    public void customizeNavigatorUserWritesTheCodeReloadShowsIt() throws Exception {
        exec("UiWebMCP.uiWebMCPDropCustomization");
        try {
            loadWithAgent();
            Locator folder = page.locator("[lsfusion-container='UiWebMCP.uiWebMCPFolder']");

            assertEquals("", call("lsfusion_exec", "{\"action\":\"SystemEvents.customizeNavigatorUser\","
                    + "\"params\":[\"NAVIGATOR { UiWebMCP.uiWebMCPFolder 'Renamed by the agent'; }\"]}", VALUES));
            assertThat(folder).containsText("UI WebMCP"); // the client keeps the navigator it was started with

            // reload loads the page again before the call answers
            page.evaluate("() => { Promise.resolve(window.uiWebMCPTools.lsfusion_exec.execute({ action: 'System.reload' })).catch(() => {}); }");
            assertThat(folder).containsText("Renamed by the agent", new LocatorAssertions.ContainsTextOptions().setTimeout(30_000));
        } finally {
            exec("UiWebMCP.uiWebMCPDropCustomization");
        }
    }

    // the agent addresses a form as ACTIVATE FORM does - by its name, the label it was opened with, the window - and
    // the script runs in that one, whichever form the user is in. The address is to name one open form
    @Test
    public void evalRunsInTheFormTheAgentAddresses() throws Exception {
        String saved = "saved " + System.nanoTime(), draft = "draft " + System.nanoTime();
        exec("UiWebMCP.uiWebMCPSeed", saved);
        loadWithAgent();
        String mode = pinMode();
        try {
            pinMode(ALL_PINNED); // a second form is opened from the navigator
            open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCP");
            Locator other = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPOther"); // the user is in this one now
            assertThat(other.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input")).hasValue(saved);
            // the page says a form's address on the form's element: this one was opened under no label
            assertEquals("System.forms", other.getAttribute("lsfusion-form-window"));
            assertEquals(null, other.getAttribute("lsfusion-form-label"));

            String first = "\"form\":{\"name\":\"UiWebMCP.uiWebMCP\"}";
            String read = "\"script\":\"run() { RETURN UiWebMCP.uiWebMCPNote(); }\"";
            assertEquals("",
                    call("lsfusion_eval", "{\"script\":\"run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; }\",\"params\":[\"" + draft + "\"]," + first + "}", VALUES));
            assertEquals(draft, call("lsfusion_eval", "{" + read + "," + first + "}", VALUES));
            assertEquals(saved, call("lsfusion_eval", "{" + read + ",\"form\":{}}", VALUES)); // the form the user is in has no draft

            // the first form again, under a label: the name alone names two forms now, the label one
            open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPLabelled");
            Locator labelled = page.locator("[lsfusion-form='UiWebMCP.uiWebMCP'][lsfusion-form-label='labelled'][lsfusion-form-window='System.forms']");
            assertThat(labelled).isVisible();
            assertEquals(saved, call("lsfusion_eval", "{" + read + ",\"form\":{\"label\":\"labelled\"}}", VALUES));
            assertEquals(saved,
                    call("lsfusion_eval", "{" + read + ",\"form\":{\"name\":\"UiWebMCP.uiWebMCP\",\"label\":\"labelled\",\"window\":\"System.forms\"}}", VALUES));
            assertEquals("lsfusion_eval error: 2 open forms at the address {\"name\":\"UiWebMCP.uiWebMCP\"}", call("lsfusion_eval", "{" + read + "," + first + "}"));
            // the empty label names the one opened with none, as in ACTIVATE FORM
            assertEquals(draft, call("lsfusion_eval", "{" + read + ",\"form\":{\"name\":\"UiWebMCP.uiWebMCP\",\"label\":\"\"}}", VALUES));
            // ... and none: a form that is not open, a label no form has, a window that is not there
            for (String none : new String[] {"{\"name\":\"UiWebMCP.uiWebMCPNoSuchForm\"}", "{\"name\":\"UiWebMCP.uiWebMCPOther\",\"label\":\"labelled\"}",
                    "{\"name\":\"UiWebMCP.uiWebMCPOther\",\"window\":\"UiWebMCP.uiWebMCPNoSuchWindow\"}"})
                assertEquals("lsfusion_eval error: no open form at the address " + none, call("lsfusion_eval", "{" + read + ",\"form\":" + none + "}"));

            // what is no address the page says itself: the server takes no `form`
            List<String> onServer = toolCalls();
            assertEquals("lsfusion_eval error: `form` must be an object of name, label, window, each a string - `{}` for the form the user is in",
                    call("lsfusion_eval", "{" + read + ",\"form\":\"UiWebMCP.uiWebMCP\"}"));
            assertEquals("lsfusion_eval error: `form`: unknown field `id`; a form is addressed by `name`, `label`, `window`",
                    call("lsfusion_eval", "{" + read + ",\"form\":{\"id\":\"1\"}}"));
            assertEquals("lsfusion_eval error: `form.name` must be a string", call("lsfusion_eval", "{" + read + ",\"form\":{\"name\":7}}"));
            assertTrue(onServer.toString(), onServer.isEmpty());
        } finally {
            pinMode(mode);
        }
    }

    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    // what the tool is called with and what it answers are the server tool's, whatever runs it: the same call answers
    // the same, run by the page where the user works and by the server
    @Test
    public void evalAnswersAsTheServersToolDoes() throws Exception {
        exec("UiWebMCP.uiWebMCPSeed", "marks");
        loadWithAgent();
        String sum = "run(CSVFILE f) { LOCAL name = STRING[50] (INTEGER); LOCAL qty = INTEGER (INTEGER); IMPORT CSV ',' NOHEADER FROM f TO name, qty; RETURN (GROUP SUM qty(INTEGER i)); }";
        String png = "{\"script\":\"run(FILE f) { RETURN f; }\",\"params\":[{\"base64\":\"" + PNG + "\",\"fileName\":\"dot.png\"}]}";
        String json = "{\"script\":\"run() { EXPORT JSON FROM a = 1, b = 'x'; }\"}";
        String marks = "{\"script\":\"run() { RETURN UiWebMCP.uiWebMCPMark(STRING[10] name); }\"}";
        for (String arguments : new String[] {
                "{\"script\":\"run(INTEGER x) { RETURN x * 2; }\",\"params\":[21]}",
                "{\"script\":\"run(INTEGER x) { RETURN x * 2; }\",\"params\":[\"21\"]}",
                "{\"script\":\"run(INTEGER x) { RETURN x IF x > 100; }\",\"params\":[21]}",
                "{\"script\":\"run(INTEGER x, STRING[10] s) { RETURN s IF NOT x; }\",\"params\":[null,\"none\"]}",
                "{\"script\":\"run(NUMERIC[10,2] x) { RETURN x * 2; }\",\"params\":[1.5]}",
                "{\"script\":\"run(LONG l) { RETURN l; }\",\"params\":[\"9007199254740993\"]}", // past what a JS number holds
                "{\"script\":\"run(STRING[20] s, BOOLEAN b) { RETURN s IF b; }\",\"params\":[\"a string\",true]}",
                "{\"script\":\"run(STRING[20] s, BOOLEAN b) { RETURN s IF NOT b; }\",\"params\":[\"a string\",\"false\"]}",
                "{\"script\":\"run(DATE d) { RETURN d; }\",\"params\":[\"2026-11-13\"]}",
                "{\"script\":\"run() { RETURN DATETIME(2026_11_13); }\"}",
                "{\"script\":\"run(JSON j) { RETURN j; }\",\"params\":[{\"a\":[1,null],\"__proto__\":{\"b\":2}}]}",
                "{\"script\":\"run(STRING s) { RETURN s; }\",\"params\":[[1,\"two\"]]}",
                "{\"script\":\"run(STRING s) { RETURN s; }\",\"params\":[1e-7]}", // a number is read as JSON has it, not as a double
                "{\"script\":\"run() { }\"}",
                json,
                "{\"script\":\"run() { EXPORT CSV FROM a = 1, b = 'x'; }\"}",
                marks, // every value of a result with a parameter, with its name
                "{\"script\":\"" + sum + "\",\"params\":[\"apples,3\\npears,4\\n\"]}", // a file is given as its text, or described
                "{\"script\":\"" + sum + "\",\"params\":[{\"text\":\"apples,3\\npears,4\\n\",\"fileName\":\"order.csv\"}]}",
                png,
                "{\"script\":\"run(NAMEDFILE f) { RETURN f; }\",\"params\":[{\"text\":\"a note\",\"fileName\":\"note.txt\"}]}"
        })
            assertTrue(arguments, !answersAsTheServer(arguments).startsWith("lsfusion_eval error: "));
        // ... and what is no call of the tool is refused the same
        for (String arguments : new String[] {
                "{\"script\":\"run(FILE f) { RETURN f; }\",\"params\":[{\"type\":\"file\",\"fileName\":\"no content\"}]}",
                "{\"script\":\"run() { }\",\"params\":\"none\"}",
                "{\"script\":\"\"}",
                "{\"params\":[]}"
        })
            assertTrue(arguments, answersAsTheServer(arguments).startsWith("lsfusion_eval error: "));
        // a value the class of its parameter refuses is refused by both, each in the words of its own way to the action
        String fraction = "{\"script\":\"run(INTEGER x) { RETURN x; }\",\"params\":[21.5]}";
        for (String refusal : new String[] {call("lsfusion_eval", fraction), onServer(fraction)})
            assertTrue(refusal, refusal.startsWith("lsfusion_eval error: ") && refusal.contains("NumberFormatException"));

        assertEquals("7", call("lsfusion_eval", "{\"script\":\"" + sum + "\",\"params\":[\"apples,3\\npears,4\\n\"]}", VALUES));
        assertEquals("first=1,second=2", call("lsfusion_eval", marks, "answer => answer.results.map(entry => entry.name + '=' + entry.value).sort().join()"));
        String file = "answer => { const file = answer.results[0]; return [file.type, file.fileName, file.extension, file.mimeType, file.size, file.base64 || file.text].join(' '); }";
        assertEquals("file file.png png image/png 70 " + PNG, call("lsfusion_eval", png, file));
        assertEquals("file file.json json application/json 15 {\"a\":1,\"b\":\"x\"}", call("lsfusion_eval", json, file));
    }

    // what the page's run of the tool, in the navigator, answers, when the server's run of it answers the same
    private String answersAsTheServer(String arguments) {
        String onThePage = call("lsfusion_eval", arguments);
        assertEquals(arguments, onServer(arguments), onThePage);
        return onThePage;
    }

    // what the server's run of the tool answers, asked as an MCP client asks
    private String onServer(String arguments) {
        return (String) page.evaluate("args => window.uiWebMCPServer('tools/call', { name: 'lsfusion_eval', arguments: JSON.parse(args) })"
                + ".then(result => window.uiWebMCPText(result.isError ? result : result.structuredContent))", arguments);
    }

    // a file too big to be in the answer is a link in it, which gives the file - each of several
    @Test
    public void aBigFileIsALink() {
        loadWithAgent();
        assertEquals("1 file scan.pdf pdf application/pdf 100000 200 100000, 2 file scan.pdf pdf application/pdf 100000 200 100000", String.valueOf(page.evaluate("() => {"
                + " const bytes = new Uint8Array(100000); for (let i = 0; i < bytes.length; i++) bytes[i] = (i * 31 + 7) % 251;"
                + " let binary = ''; for (let i = 0; i < bytes.length; i += 8192) binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 8192));"
                + " const script = 'run(NAMEDFILE f) { LOCAL g = NAMEDFILE (INTEGER); g(1) <- f; g(2) <- f; RETURN g(INTEGER i); }';"
                + " return Promise.resolve(window.uiWebMCPTools.lsfusion_eval.execute({ script, params: [{ base64: btoa(binary), fileName: 'scan.pdf' }] })).then(answer => {"
                + "   if (answer.isError) return answer.content[0].text;"
                + "   return Promise.all(answer.results.map(file => fetch(file.url).then(response => response.arrayBuffer().then(got =>"
                + "     [file.name, file.type, file.fileName, file.extension, file.mimeType, file.size, response.status,"
                + "      new Uint8Array(got).every((value, i) => value === bytes[i]) ? got.byteLength : 'other bytes'].join(' '))))).then(files => files.sort().join(', '));"
                + " }); }")));
    }

    private static final String CLIENT_TOOL = "lsfusion_client";

    // a method of the form the user is in
    private String callForm(String method, String args) {
        return call(CLIENT_TOOL, "{\"method\":\"" + method + "\",\"args\":" + args + ",\"form\":{}}");
    }

    // the page's own tool calls a method of the form the user is in, by the method's name, its arguments by theirs:
    // what the form's toolbar, sortings, filters and cells do, which the form shows. A value is JSON, a date its string
    @Test
    public void theClientToolCallsAMethodOfTheForm() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        assertEquals(CLIENT_TOOL + " error: the user is in no form", callForm("getCount", "{\"group\":\"i\"}"));
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
        Locator names = form.locator("td[lsfusion-container='PROPERTY(name)']");
        assertThat(names).hasCount(3);

        // what the toolbar counts, the answer the method's
        assertEquals("{\"result\":3}", callForm("getCount", "{\"group\":\"i\"}"));
        assertEquals("{\"result\":55}", callForm("getSum", "{\"group\":\"i\",\"property\":\"quantity\"}"));

        // the sortings: all of them, and one of them
        assertEquals("{}", callForm("changeOrders", "{\"table\":\"i\",\"orders\":[{\"property\":\"quantity\",\"desc\":true}]}"));
        assertThat(names).hasText(new String[] {"c", "b", "a"});
        assertEquals("{}", callForm("changeOrders", "{\"table\":\"i\",\"orders\":{\"property\":\"quantity\",\"desc\":false}}"));
        assertThat(names).hasText(new String[] {"a", "b", "c"});

        // the user filters: all the conditions, one more - on a date, given as its string -, and none
        assertEquals("{}", callForm("changeFilters", "{\"table\":\"i\",\"conditions\":[{\"property\":\"quantity\",\"compare\":\">\",\"value\":10}]}"));
        assertThat(names).hasText(new String[] {"b", "c"});
        assertEquals("{}", callForm("changeFilters", "{\"table\":\"i\",\"conditions\":{\"property\":\"date\",\"compare\":\">=\",\"value\":\"2026-03-01\"}}"));
        assertThat(names).hasText(new String[] {"c"});
        assertEquals("{\"result\":1}", callForm("getCount", "{\"group\":\"i\"}"));
        assertEquals("{}", callForm("changeFilters", "{\"table\":\"i\",\"conditions\":[]}"));
        assertThat(names).hasText(new String[] {"a", "b", "c"});

        // a filter group's filter, selected and not
        assertEquals("{}", callForm("changeFilterGroup", "{\"filterGroup\":\"many\",\"index\":0}"));
        assertThat(names).hasText(new String[] {"b", "c"});
        assertEquals("{}", callForm("changeFilterGroup", "{\"filterGroup\":\"many\",\"index\":null}"));
        assertThat(names).hasText(new String[] {"a", "b", "c"});

        // a property of the current row: its value set - a date's by its string -, and its change run, an action's
        select(cell(form, "name", "b"));
        Locator row = row(form, "name", "b");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.quantity\",\"value\":21}"));
        assertThat(row.locator("td[lsfusion-container='PROPERTY(quantity)']")).hasText("21");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.date\",\"value\":\"2026-12-31\"}"));
        assertEquals("1", call("lsfusion_eval", "{\"script\":\"run() { RETURN GROUP SUM 1 IF UiWebMCP.uiWebMCPDate(UiWebMCP.UiWebMCPItem i) = 2026_12_31; }\",\"form\":{}}", VALUES));
        Locator note = form.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.touch\"}"));
        assertThat(note).hasValue("touched b");
        // ... and one of no group, set and set to none
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"uiWebMCPNote\",\"value\":\"noted\"}"));
        assertThat(note).hasValue("noted");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"uiWebMCPNote\",\"value\":null}"));
        assertThat(note).hasValue("");

        // the values the form suggests for a property, each as its strings
        assertEquals("[{\"displayString\":\"a\",\"rawString\":\"a\"},{\"displayString\":\"b\",\"rawString\":\"b\"},{\"displayString\":\"c\",\"rawString\":\"c\"}]",
                call(CLIENT_TOOL, "{\"method\":\"getValues\",\"args\":{\"property\":\"i.name\",\"value\":\"\",\"options\":{\"mode\":\"values\"}},\"form\":{}}",
                        "answer => JSON.stringify(window.uiWebMCPShape(answer.result).sort((a, b) => a.rawString < b.rawString ? -1 : 1))")); // in the server's order

        // a tree's group, opened to the last node and closed
        Locator nodes = form.locator("td[lsfusion-container='PROPERTY(node)']");
        assertThat(nodes).hasText(new String[] {"a", "b"});
        assertEquals("{}", callForm("expandAll", "{\"group\":\"t\"}"));
        assertThat(nodes).hasText(new String[] {"a", "c", "b"});
        assertEquals("{}", callForm("collapseAll", "{\"group\":\"t\"}"));
        assertThat(nodes).hasText(new String[] {"a", "b"});
    }

    // what is no call of the tool the page says, there being no server to say it; and what the form refuses is the
    // tool's failure, with the form's reason
    @Test
    public void theClientToolSaysWhatIsWrongWithACall() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
        assertThat(form.locator("td[lsfusion-container='PROPERTY(name)']")).hasCount(3); // the user is in the form
        String error = CLIENT_TOOL + " error: ";
        assertEquals(error + "the call must be an object of method, args, batch, form", call(CLIENT_TOOL, "null"));
        assertEquals(error + "unknown field `group`: the call has method, args, batch, form", call(CLIENT_TOOL, "{\"method\":\"getCount\",\"group\":\"i\"}"));
        // a method that takes an element is not one of the tool's
        String methods = callForm("startEditing", "{}");
        assertTrue(methods, methods.startsWith(error + "`method` must be one of changeObject, changeProperty, getValues, ") && !methods.contains("startEditing"));
        // ... and whose a method is, the form says: a form's method with no form, the navigator's with one
        assertEquals(error + "`getCount` is a method of a form: it is called with `form` - `{}` for the form the user is in",
                call(CLIENT_TOOL, "{\"method\":\"getCount\",\"args\":{\"group\":\"i\"}}"));
        // ... a name both have, `activate`, by the argument that is the other's
        assertEquals(error + "`activate` with `element` is a method of the navigator: it is called with no `form`", callForm("activate", "{\"element\":\"UiWebMCP.uiWebMCP\"}"));
        assertEquals(error + "unknown field `group`: `args` of close has no fields", callForm("close", "{\"group\":\"i\"}"));
        assertEquals(error + "`method` must be one of activate - or, with `form`, a method of a form", call(CLIENT_TOOL, "{\"method\":\"startEditing\"}"));
        assertEquals(error + "unknown field `table`: `args` of getCount has group", callForm("getCount", "{\"table\":\"i\"}"));
        assertEquals(error + "`args` of getCount must be an object of group", callForm("getCount", "[\"i\"]"));
        assertEquals(error + "`form` must be an object of name, label, window, each a string - `{}` for the form the user is in",
                call(CLIENT_TOOL, "{\"method\":\"getCount\",\"args\":{\"group\":\"i\"},\"form\":\"UiWebMCP.uiWebMCPItems\"}"));

        for (String[] refused : new String[][] {
                {"getCount", "{\"group\":\"none\"}", "unknown object group or tree 'none'"},
                {"getCount", "{}", "takes the name of an object group or of a tree, as a string"},
                {"changeOrders", "{\"table\":\"i\",\"orders\":[{\"property\":\"none\",\"desc\":true}]}", "order 0: unknown property 'none' of object group 'i'"},
                {"changeProperty", "{\"property\":\"i.date\",\"value\":\"31.12.2026\"}", "\"31.12.2026\" is not a date"},
                {"changeProperty", "{\"property\":\"i.date\",\"value\":\"2026-02-30\"}", "\"2026-02-30\" is not a date"},
                // a tree has no first row to move to, and no selection
                {"changeObject", "{\"group\":\"t\",\"row\":{\"to\":\"end\"}}", "'t' has no selection"},
                {"selectAll", "{\"group\":\"t\"}", "'t' has no selection"}}) {
            String refusal = callForm(refused[0], refused[1]);
            assertTrue(refusal, refusal.startsWith(error) && refusal.contains(refused[2]));
        }
    }

    // several calls of methods of the form the user is in, in one: `methodsAndArgs` is a method's name, then its arguments
    private String callFormMethods(String... methodsAndArgs) {
        List<String> calls = new ArrayList<>();
        for (int i = 0; i < methodsAndArgs.length; i += 2)
            calls.add("{\"method\":\"" + methodsAndArgs[i] + "\",\"args\":" + methodsAndArgs[i + 1] + "}");
        return call(CLIENT_TOOL, "{\"batch\":[" + String.join(",", calls) + "],\"form\":{}}");
    }

    // several calls in one, `batch`: made in the form's batch, their changes sent in one request in their order, each
    // seeing what the calls before it changed as the page has it - a row made the current one is the row of a change
    // given none. Every call is checked before any is made - a method that answers something is called alone -, and the
    // first one that fails stops them: on the page - a throw -, what the calls before it changed is sent, the calls
    // after it are not made; on the server, what the server made before the failure stays in the form's session
    @Test
    public void theClientToolMakesCallsInOneBatch() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
        Locator names = form.locator("td[lsfusion-container='PROPERTY(name)']");
        assertThat(names).hasCount(3);
        String error = CLIENT_TOOL + " error: ";
        String desc = "{\"table\":\"i\",\"orders\":[{\"property\":\"quantity\",\"desc\":true}]}";
        String many = "{\"table\":\"i\",\"conditions\":[{\"property\":\"quantity\",\"compare\":\">\",\"value\":10}]}";

        // a filter and a sorting, a row made the current one and a value changed in it: one request
        String b = row(form, "name", "b").getAttribute("lsfusion-row");
        List<String> requests = requests(request -> request.method().equals("POST"));
        assertEquals("{}", callFormMethods(
                "changeFilters", many,
                "changeOrders", desc,
                "changeObject", "{\"group\":\"i\",\"row\":\"" + b + "\"}",
                "changeProperty", "{\"property\":\"i.quantity\",\"value\":21}"));
        assertThat(names).hasText(new String[] {"c", "b"});
        assertThat(row(form, "name", "b").locator("td[lsfusion-container='PROPERTY(quantity)']")).hasText("21");
        assertEquals(1, count(requests, "ExecuteBatch"));
        // ... none of the four apart from it - a request the form makes of itself, as a page size after a resize, may go
        assertEquals(requests.toString(), 0, requests.stream().filter(body -> !body.contains(signature("ExecuteBatch")) && Stream.of("SetUserFilters",
                "SetPropertyOrders", "ChangeGroupObject", "ExecuteEventAction").anyMatch(kind -> body.contains(signature(kind)))).count());
        assertEquals("{\"result\":51}", callForm("getSum", "{\"group\":\"i\",\"property\":\"quantity\"}")); // the one call, as it was

        // a call that is none, or that answers something, is refused before any is made: the sorting is not changed back
        assertEquals(error + "`batch[1]`: `getCount` is not made in `batch`, which carries changes only: it is called alone, with `method`",
                callFormMethods("changeOrders", "{\"table\":\"i\",\"orders\":[]}", "getCount", "{\"group\":\"i\"}"));
        assertEquals(error + "`batch[1]`: unknown field `table`: `args` of expandAll has group",
                callFormMethods("changeOrders", "{\"table\":\"i\",\"orders\":[]}", "expandAll", "{\"table\":\"i\"}"));
        assertEquals(error + "`batch[0]`: unknown field `form`: a call has method, args",
                call(CLIENT_TOOL, "{\"batch\":[{\"method\":\"update\",\"args\":{\"group\":\"i\"},\"form\":{}}],\"form\":{}}"));
        assertThat(names).hasText(new String[] {"c", "b"});
        assertEquals(error + "the call has `method` and `args`, or `batch`, not both",
                call(CLIENT_TOOL, "{\"method\":\"update\",\"batch\":[],\"form\":{}}"));
        assertEquals(error + "`batch` must be a non-empty array of calls, each an object of method, args", call(CLIENT_TOOL, "{\"batch\":[],\"form\":{}}"));
        assertEquals(error + "`batch` contains calls of a form's methods: it is given with `form` - `{}` for the form the user is in",
                call(CLIENT_TOOL, "{\"batch\":[{\"method\":\"update\",\"args\":{\"group\":\"i\"}}]}"));

        // a call that throws - which the controller's method does with a property the group has not: the calls before
        // it sent, the one after it not made - the sorting is not changed
        page.evaluate("() => { window.uiWebMCPKeep = controller => { window.uiWebMCPController = controller; }; }");
        assertEquals("", call("lsfusion_eval", "{\"script\":\"run() { INTERNAL CLIENT 'uiWebMCPKeep'; }\",\"form\":{}}", VALUES));
        assertEquals("thrown", page.evaluate("() => { try { window.uiWebMCPController.changeOrders('i', [{property: 'none', desc: true}]); return 'not thrown'; }"
                + " catch (e) { return 'thrown'; } }"));
        String thrown = callFormMethods(
                "changeFilters", "{\"table\":\"i\",\"conditions\":[]}",
                "changeOrders", "{\"table\":\"i\",\"orders\":[{\"property\":\"none\",\"desc\":true}]}",
                "changeOrders", "{\"table\":\"i\",\"orders\":[]}");
        assertTrue(thrown, thrown.startsWith(error + "`batch[1]` (changeOrders) failed: ") && thrown.contains("unknown property 'none'")
                && thrown.endsWith(". The calls before it were made, in one request. The calls after it were not made."));
        assertThat(names).hasText(new String[] {"c", "b", "a"});

        // a request the server fails: what it made before the failure stays done
        String failed = callFormMethods(
                "changeProperty", "{\"property\":\"uiWebMCPNote\",\"value\":\"before the failure\"}",
                "changeProperty", "{\"property\":\"uiWebMCPCrash\"}");
        assertTrue(failed, failed.startsWith(error + "The server failed the calls' request: ") && failed.contains("uiWebMCPCrash: the action failed")
                && failed.endsWith(". What the server made before the failure stays done, nothing is taken back."));
        closeDialog("uiWebMCPCrash: the action failed");
        reported("uiWebMCPCrash: the action failed");
        assertEquals("before the failure", call("lsfusion_eval", "{\"script\":\"run() { RETURN UiWebMCP.uiWebMCPNote(); }\",\"form\":{}}", VALUES));
    }

    // what the agent names it reads on the page: a property by the name on what shows it, a row by the group and the
    // key on its element. A row it sees it makes the current one, whose properties it then changes, and a node of a
    // tree it opens and closes; a key that is no row the form has of the group is refused
    @Test
    public void theClientToolTakesARowAsThePageShowsIt() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
        Locator names = form.locator("td[lsfusion-container='PROPERTY(name)']");
        assertThat(names).hasCount(3);

        // a property's name as the methods take it, on its cells, on its column's header and on its field
        assertThat(names.first()).hasAttribute("lsfusion-property", "i.name");
        assertThat(form.locator("th[lsfusion-property='i.quantity']")).hasCount(1);
        assertThat(form.locator("td[lsfusion-property='t.node']")).hasCount(2);
        Locator note = form.locator("[lsfusion-property='uiWebMCPNote'] input");
        assertThat(note).hasCount(1);

        // a row of the grid made the current one, by its key - a string as the page has it, and the number it is
        Locator b = row(form, "name", "b"), c = row(form, "name", "c");
        assertThat(b).hasAttribute("lsfusion-group", "i");
        String cKey = c.getAttribute("lsfusion-row");
        assertEquals("{}", callForm("changeObject", "{\"group\":\"i\",\"row\":\"" + b.getAttribute("lsfusion-row") + "\"}"));
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.touch\"}"));
        assertThat(note).hasValue("touched b");
        assertEquals("{}", callForm("changeObject", "{\"group\":\"i\",\"row\":" + cKey + "}"));
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.quantity\",\"value\":31}"));
        assertThat(c.locator("td[lsfusion-container='PROPERTY(quantity)']")).hasText("31");
        // ... and a property changed in a row that is not the current one, by the row's key beside the property: its
        // value set, its change run, its suggestions asked - the current row staying the one it was
        String bKey = b.getAttribute("lsfusion-row");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.quantity\",\"row\":" + bKey + ",\"value\":22}"));
        assertThat(b.locator("td[lsfusion-container='PROPERTY(quantity)']")).hasText("22");
        assertThat(c.locator("td[lsfusion-container='PROPERTY(quantity)']")).hasText("31");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.touch\",\"row\":\"" + bKey + "\"}"));
        assertThat(note).hasValue("touched b");
        assertEquals("{}", callForm("changeProperty", "{\"property\":\"i.touch\"}")); // the current row is still c
        assertThat(note).hasValue("touched c");
        String suggested = call(CLIENT_TOOL, "{\"method\":\"getValues\",\"args\":{\"property\":\"i.name\",\"row\":" + bKey + ",\"value\":\"\",\"options\":{\"mode\":\"values\"}},\"form\":{}}",
                "answer => JSON.stringify(answer.result.map(value => value.rawString).sort())");
        assertEquals("[\"a\",\"b\",\"c\"]", suggested);
        // ... the key staying the row's as its element is used for another row: the rows sorted the other way round
        assertEquals("{}", callForm("changeOrders", "{\"table\":\"i\",\"orders\":[{\"property\":\"name\",\"desc\":true}]}"));
        assertThat(names).hasText(new String[] {"c", "b", "a"});
        assertThat(form.locator("tr[lsfusion-group='i']").first()).hasAttribute("lsfusion-row", cKey);
        assertThat(c).hasAttribute("lsfusion-row", cKey);

        // a node of the tree: a row of its group once the form has it - not before its node is opened, nor after that
        // is closed
        Locator nodes = form.locator("td[lsfusion-container='PROPERTY(node)']");
        assertThat(nodes).hasText(new String[] {"a", "b"});
        Locator a = row(form, "node", "a");
        assertThat(a).hasAttribute("lsfusion-group", "t");
        String aKey = a.getAttribute("lsfusion-row");
        String error = CLIENT_TOOL + " error: ";
        String noRow = error + "'" + cKey + "' is no row the form has of group 't': a row is named by the `lsfusion-row` of its element";
        assertEquals(noRow, callForm("changeObject", "{\"group\":\"t\",\"row\":" + cKey + "}"));
        assertEquals("{}", callForm("expand", "{\"group\":\"t\",\"row\":" + aKey + "}"));
        assertThat(nodes).hasText(new String[] {"a", "c", "b"});
        assertThat(row(form, "node", "c")).hasAttribute("lsfusion-row", cKey);
        assertEquals("{}", callForm("changeObject", "{\"group\":\"t\",\"row\":" + cKey + "}"));
        assertEquals("{}", callForm("collapse", "{\"group\":\"t\",\"row\":" + aKey + "}"));
        assertThat(nodes).hasText(new String[] {"a", "b"});
        assertEquals(noRow, callForm("changeObject", "{\"group\":\"t\",\"row\":" + cKey + "}"));

        // ... and what the method says of the row, and of a group that is none
        for (String[] refused : new String[][] {
                {"expand", "{\"group\":\"i\",\"row\":" + cKey + "}", "'i' is not a group of a tree"},
                {"changeObject", "{\"group\":\"none\",\"row\":" + cKey + "}", "unknown object group 'none'"},
                {"changeObject", "{\"group\":\"i\"}", "expects a row of 'i'"},
                // ... beside a property: a key that is no row of the property's group, and a property that has no rows
                {"changeProperty", "{\"property\":\"i.quantity\",\"row\":0,\"value\":1}", "'0' is no row the form has of group 'i'"},
                {"getValues", "{\"property\":\"i.name\",\"row\":0,\"value\":\"\"}", "'0' is no row the form has of group 'i'"},
                {"changeProperty", "{\"property\":\"uiWebMCPNote\",\"row\":" + cKey + ",\"value\":\"x\"}", "'uiWebMCPNote' names a property of the form level, which has no rows"},
                // ... with no value too, where the key would be taken for the value
                {"changeProperty", "{\"property\":\"uiWebMCPNote\",\"row\":" + cKey + "}", "'uiWebMCPNote' names a property of the form level, which has no rows"},
                // ... and a row that is no key: taken for the value too
                {"changeProperty", "{\"property\":\"i.quantity\",\"row\":{\"to\":\"end\"}}", "`row` must be the key of a row, a string or a number"},
                {"getValues", "{\"property\":\"i.name\",\"row\":" + cKey + ",\"value\":\"\",\"options\":[]}", "the options argument must be an object"}}) {
            String refusal = callForm(refused[0], refused[1]);
            assertTrue(refusal, refusal.startsWith(error) && refusal.contains(refused[2]));
        }
    }

    // ... and a row a React view draws, which has the key in its rows: the tool finds it among the rows the form has,
    // whoever draws them
    @Test
    public void theClientToolTakesARowAReactViewDraws() throws Exception {
        exec("UiReactGrid.uiRgSeed", "5");
        loadWithAgent();
        open("UiReactGrid.uiRg", "UiReactGrid.uiRgGrid");
        View view = view("window.uiRgView", "const rows = data && data.i ? data.i.list : [], current = rows.find(row => row.current);");
        view.waitFor("current && current.number.value === 1");
        assertEquals("{}", callForm("changeObject", "{\"group\":\"i\",\"row\":" + view.value("String(rows[2].key)") + "}"));
        view.waitFor("current.number.value === 3");
        assertEquals(CLIENT_TOOL + " error: '0' is no row the form has of group 'i': a row is named by the `lsfusion-row` of its element",
                callForm("changeObject", "{\"group\":\"i\",\"row\":0}"));
    }

    // the selection of a table and its clipboard, the table's own where the platform draws it: a row and a column made
    // current with what becomes of the rows and of the columns selected, the cells selected answered as a text - the
    // current row's while no row is selected -, a value pasted into every one of them, every row and column selected
    @Test
    public void theClientToolSelectsCopiesAndPastes() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
        Locator rows = form.locator("tr[lsfusion-group='i']");
        assertThat(rows).hasCount(3);
        assertEquals("{}", callForm("changeOrders", "{\"table\":\"i\",\"orders\":[{\"property\":\"name\",\"desc\":false}]}"));
        assertThat(form.locator("td[lsfusion-container='PROPERTY(name)']")).hasText(new String[] {"a", "b", "c"});
        String first = rows.nth(0).getAttribute("lsfusion-row"), last = rows.nth(2).getAttribute("lsfusion-row");

        assertEquals("{}", callForm("changeObject", "{\"group\":\"i\",\"row\":" + first + "}"));
        assertEquals("{}", callForm("changeColumn", "{\"group\":\"i\",\"column\":\"name\"}"));
        assertEquals("{}", callForm("changeColumn", "{\"group\":\"i\",\"column\":\"quantity\",\"options\":{\"selection\":\"extend\"}}"));
        assertEquals("{\"result\":\"a\\t5\"}", callForm("copy", "{\"group\":\"i\"}"));
        assertEquals("{}", callForm("changeObject", "{\"group\":\"i\",\"row\":" + last + ",\"options\":{\"selection\":\"extend\"}}"));
        assertThat(form.locator("tr.table-select")).hasCount(3);
        assertEquals("{\"result\":\"a\\t5\\nb\\t20\\nc\\t30\"}", callForm("copy", "{\"group\":\"i\"}"));

        assertEquals("{}", callForm("changeColumn", "{\"group\":\"i\",\"column\":\"quantity\"}"));
        assertEquals("{}", callForm("paste", "{\"group\":\"i\",\"text\":\"7\"}"));
        assertThat(form.locator("td[lsfusion-container='PROPERTY(quantity)']")).hasText(new String[] {"7", "7", "7"});
        assertEquals("{}", callForm("changeObject", "{\"group\":\"i\",\"row\":" + first + "}")); // a selection anew: no row
        assertThat(form.locator("tr.table-select")).hasCount(0);
        assertEquals("{\"result\":\"7\"}", callForm("copy", "{\"group\":\"i\"}"));
        assertEquals("{}", callForm("selectAll", "{\"group\":\"i\"}"));
        assertThat(form.locator("tr.table-select")).hasCount(3);
        String all = callForm("copy", "{\"group\":\"i\"}");
        assertTrue(all, all.startsWith("{\"result\":\"a\\t7\\t") && all.split("\\\\n").length == 3);
    }

    // the form is the one the agent addresses, whichever form the user is in
    @Test
    public void theClientToolCallsTheFormTheAgentAddresses() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        String mode = pinMode();
        try {
            pinMode(ALL_PINNED); // a second form is opened from the navigator
            Locator items = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
            assertThat(items.locator("td[lsfusion-container='PROPERTY(name)']")).hasCount(3);
            Locator other = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPOther"); // the user is in this one now
            assertThat(other.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input")).isVisible();
            String count = "{\"method\":\"getCount\",\"args\":{\"group\":\"i\"}";
            String here = call(CLIENT_TOOL, count + ",\"form\":{}}");
            assertTrue(here, here.startsWith(CLIENT_TOOL + " error: ") && here.contains("unknown object group or tree 'i'"));
            assertEquals("{\"result\":3}", call(CLIENT_TOOL, count + ",\"form\":{\"name\":\"UiWebMCP.uiWebMCPItems\"}}"));
            assertEquals(CLIENT_TOOL + " error: no open form at the address {\"name\":\"UiWebMCP.uiWebMCPNoSuchForm\"}",
                    call(CLIENT_TOOL, count + ",\"form\":{\"name\":\"UiWebMCP.uiWebMCPNoSuchForm\"}}"));
        } finally {
            pinMode(mode);
        }
    }

    // the form itself: made the current one - the form the user is in from then on - and closed, the one the agent
    // addresses and the one the user is in
    @Test
    public void theClientToolActivatesAndClosesAForm() throws Exception {
        exec("UiWebMCP.uiWebMCPSeedItems");
        loadWithAgent();
        String mode = pinMode();
        try {
            pinMode(ALL_PINNED); // a second form is opened from the navigator
            Locator items = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPItems");
            assertThat(items.locator("td[lsfusion-container='PROPERTY(name)']")).hasCount(3);
            Locator other = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPOther"); // the user is in this one now
            assertThat(other.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input")).isVisible();
            String count = "{\"method\":\"getCount\",\"args\":{\"group\":\"i\"},\"form\":{}}";
            String here = call(CLIENT_TOOL, count);
            assertTrue(here, here.contains("unknown object group or tree 'i'"));

            assertEquals("{}", call(CLIENT_TOOL, "{\"method\":\"activate\",\"form\":{\"name\":\"UiWebMCP.uiWebMCPItems\"}}"));
            assertEquals("{\"result\":3}", call(CLIENT_TOOL, count)); // at once: the next call is for it
            assertThat(items.locator("td[lsfusion-container='PROPERTY(name)']").first()).isVisible();

            assertEquals("{}", call(CLIENT_TOOL, "{\"method\":\"close\",\"form\":{\"name\":\"UiWebMCP.uiWebMCPOther\"}}"));
            assertThat(other).hasCount(0);
            assertEquals("{\"result\":3}", call(CLIENT_TOOL, count)); // the user is still in the first one
            // ... with an edit under way in it: the editor is committed first - the form would wait for it to end
            Locator quantity = row(items, "name", "b").locator("td[lsfusion-container='PROPERTY(quantity)']");
            quantity.dblclick();
            assertThat(quantity.locator("input")).isVisible();
            assertEquals("{}", callForm("close", "{}"));
            assertThat(items).hasCount(0);
            assertEquals(CLIENT_TOOL + " error: the user is in no form", call(CLIENT_TOOL, count));
        } finally {
            pinMode(mode);
        }
    }

    // with no form the method is the navigator's: an element of the navigator activated by its name, which the page
    // shows on it (`lsfusion-container`, what a test opens a form by), as a click on it does - here a form's, opened
    @Test
    public void theClientToolCallsAMethodOfTheNavigator() {
        loadWithAgent();
        assertThat(page.locator("[lsfusion-form='UiWebMCP.uiWebMCP']")).hasCount(0);
        assertEquals("{}", call(CLIENT_TOOL, "{\"method\":\"activate\",\"args\":{\"element\":\"UiWebMCP.uiWebMCP\"}}"));
        assertThat(page.locator("[lsfusion-form='UiWebMCP.uiWebMCP']")).isVisible();
        assertEquals(CLIENT_TOOL + " error: Navigator element 'UiWebMCP.uiWebMCPNoSuchElement' not found",
                call(CLIENT_TOOL, "{\"method\":\"activate\",\"args\":{\"element\":\"UiWebMCP.uiWebMCPNoSuchElement\"}}"));
        assertEquals(CLIENT_TOOL + " error: unknown field `form`: `args` of activate has element",
                call(CLIENT_TOOL, "{\"method\":\"activate\",\"args\":{\"form\":\"UiWebMCP.uiWebMCP\"}}"));
    }

    // the methods the tool calls are the controllers': every function of the form's controller, and of the
    // navigator's, is one of them, or one of those the tool leaves out - what takes an element or a function, the calls
    // of the logic, which other tools are, and a view's own filters. A method added to a controller is to be put in one
    // of the two
    @Test
    public void theClientToolsMethodsAreTheControllers() throws Exception {
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCP");
        assertThat(form.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input")).isVisible(); // the user is in the form
        // an INTERNAL CLIENT action's function is given the form's controller
        page.evaluate("() => { window.uiWebMCPKeep = controller => { window.uiWebMCPController = controller; }; }");
        assertEquals("", call("lsfusion_eval", "{\"script\":\"run() { INTERNAL CLIENT 'uiWebMCPKeep'; }\",\"form\":{}}", VALUES));
        String functions = "() => Object.keys(window.uiWebMCPController).filter(name => typeof window.uiWebMCPController[name] === 'function').sort().join()";
        // ... but what the client does with the form itself, which is no method of its controller
        String methods = "([heading, leftOut]) => { const description = window.uiWebMCPTools." + CLIENT_TOOL + ".description, from = description.indexOf(heading);"
                + " const till = description.indexOf('\\n\\n', from), own = heading.includes('form') ? ['activate', 'close'] : [];"
                + " return (description.slice(from, till < 0 ? undefined : till).match(/^- `\\w+/gm) || []).map(name => name.slice(3))"
                + "   .filter(name => !own.includes(name)).concat(leftOut).sort().join(); }";
        List<String> logic = Arrays.asList("exec", "eval", "evalAction", "change");
        List<String> leftOut = new ArrayList<>(Arrays.asList("startEditing", "stopEditing", "isEditing", "batch", "changeViewFilters"));
        leftOut.addAll(logic);
        assertEquals(page.evaluate(functions), page.evaluate(methods, Arrays.asList("**Methods of a form.**", leftOut)));
        // ... the navigator's controller is what the function gets from a script run with no form
        assertEquals("", call("lsfusion_eval", "{\"script\":\"run() { INTERNAL CLIENT 'uiWebMCPKeep'; }\"}", VALUES));
        assertEquals(page.evaluate(functions), page.evaluate(methods, Arrays.asList("**Methods of the navigator.**", logic)));
        assertEquals("activate,changeColumn", page.evaluate("() => window.uiWebMCPTools." + CLIENT_TOOL + ".inputSchema.properties.method.enum.filter(name => name === 'activate' || name === 'changeColumn').sort().join()"));
        // ... a call of a sequence names the same methods
        assertEquals(true, page.evaluate("() => { const properties = window.uiWebMCPTools." + CLIENT_TOOL + ".inputSchema.properties;"
                + " return JSON.stringify(properties.batch.items.properties.method) === JSON.stringify(properties.method); }"));
        // ... and each is described, with what it takes
        String description = (String) page.evaluate("() => window.uiWebMCPTools." + CLIENT_TOOL + ".description");
        assertTrue(description, description.contains("- `changeOrders(table, orders)` — ") && description.contains("- `export(table)` — "));
    }

    @Test
    public void aToolOfTheServerRunsOnTheServer() {
        loadWithAgent();
        String files = call("lsfusion_files_list", "{\"pathPattern\":\"**/UiWebMCP.lsf\"}");
        assertTrue(files, files.contains("\"files\":[\"/ui/UiWebMCP.lsf\"]"));
    }

    // a failure is an answer too: what a tool throws does not reach the agent
    @Test
    public void aFailureIsAnAnswer() {
        loadWithAgent();
        assertEquals("lsfusion_eval error: the user is in no form", call("lsfusion_eval", "{\"script\":\"run() { }\",\"form\":{}}"));
        String unknown = call("lsfusion_eval", "{\"script\":\"run() { RETURN UiWebMCP.uiWebMCPNoSuchProperty(); }\"}");
        assertTrue(unknown, unknown.startsWith("lsfusion_eval error: ") && unknown.contains("uiWebMCPNoSuchProperty"));
        // a field the tool does not take - the `context` it had - the page says, and what is no call at all, the server
        assertEquals("lsfusion_eval error: unknown field `context`: the call has script, params, form",
                call("lsfusion_eval", "{\"script\":\"run() { }\",\"context\":\"navigator\"}"));
        String noArguments = call("lsfusion_eval", "null");
        assertTrue(noArguments, noArguments.startsWith("lsfusion_eval error: ") && noArguments.contains("'script' is required"));
        String missing = call("lsfusion_files_read", "{\"path\":\"/ui/UiWebMCPNoSuchFile.lsf\"}");
        assertTrue(missing, missing.startsWith("lsfusion_files_read error: "));
    }
}
