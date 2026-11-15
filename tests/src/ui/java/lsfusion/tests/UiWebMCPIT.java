package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.Arrays;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// WebMCP: the page registers the tools of the application's MCP server with the browser's agent. A tool is the server's
// - its description, what it is called with and what it answers -, and the page substitutes only the run of
// lsfusion_eval where the user works: in the form the user is in or in the navigator. The agent is a stand-in here: a
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

    // where the script ran and what it returned, each result as its value
    private static final String RAN = "answer => [answer.context].concat(answer.form || [], answer.results.map(entry => entry.value)).join(' | ')";

    // the tools are the server's, their descriptions too - the one of the tool the page runs itself among them
    @Test
    public void thePageRegistersTheToolsOfTheServer() {
        loadWithAgent();
        String described = "tools => tools.map(tool => tool.name + ': ' + tool.description + JSON.stringify(window.uiWebMCPShape(tool.inputSchema))).sort().join('\\n')";
        Object listed = page.evaluate("() => window.uiWebMCPServer('tools/list').then(list => (" + described + ")(list.tools))");
        assertTrue("the server lists lsfusion_eval among its tools: " + listed, String.valueOf(listed).contains("lsfusion_eval: "));
        assertEquals(listed, page.evaluate("() => (" + described + ")(Object.keys(window.uiWebMCPTools).map(name => window.uiWebMCPTools[name]))"));
        assertEquals("form,logics,navigator", page.evaluate("() => window.uiWebMCPTools.lsfusion_eval.inputSchema.properties.context.enum.slice().sort().join()"));
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
        assertEquals("navigator | 2", call("lsfusion_eval", "{\"script\":\"run() { RETURN 1 + 1; }\"}", RAN));
        String files = call("lsfusion_files_search", "{\"regex\":\"MODULE UiWebMCP;\",\"pathPattern\":\"**/UiWebMCP.lsf\"}");
        assertTrue(files, files.contains("/ui/UiWebMCP.lsf\""));
    }

    @Test
    public void evalRunsInTheNavigatorWhenTheUserIsInNoForm() {
        loadWithAgent();
        assertEquals("navigator | 42", call("lsfusion_eval", "{\"script\":\"run(INTEGER x) { RETURN x * 2; }\",\"params\":[21]}", RAN));
        assertEquals("navigator | 42", call("lsfusion_eval", "{\"script\":\"run(INTEGER x) { RETURN x * 2; }\",\"params\":[\"21\"]}", RAN));
        assertEquals("navigator", call("lsfusion_eval", "{\"script\":\"run() { }\"}", RAN));
    }

    // the form's session holds what the script changed and did not apply, and the form shows it; a session of the
    // navigator reads what is saved
    @Test
    public void evalRunsInTheFormTheUserIsIn() throws Exception {
        String saved = "saved " + System.nanoTime(), draft = "draft " + System.nanoTime();
        exec("UiWebMCP.uiWebMCPSeed", saved);
        loadWithAgent();
        Locator form = open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCP");
        Locator note = form.locator("[lsfusion-container='PROPERTY(uiWebMCPNote())'] input");
        assertThat(note).hasValue(saved);

        String read = "run() { RETURN UiWebMCP.uiWebMCPNote(); }";
        assertEquals("form | UiWebMCP.uiWebMCP",
                call("lsfusion_eval", "{\"script\":\"run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; }\",\"params\":[\"" + draft + "\"]}", RAN));
        assertThat(note).hasValue(draft);
        assertEquals("form | UiWebMCP.uiWebMCP | " + draft, call("lsfusion_eval", "{\"script\":\"" + read + "\",\"context\":\"form\"}", RAN));
        assertEquals("navigator | " + saved, call("lsfusion_eval", "{\"script\":\"" + read + "\",\"context\":\"navigator\"}", RAN));
        // ... and so does the server, where nobody is shown a message: it is in the answer
        assertEquals(saved + " | said", call("lsfusion_eval", "{\"script\":\"run() { MESSAGE 'said'; RETURN UiWebMCP.uiWebMCPNote(); }\",\"context\":\"logics\"}",
                "answer => [].concat(answer.context || [], answer.results[0].value, answer.messages).join(' | ')"));
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
            assertEquals("form | UiWebMCP.uiWebMCP",
                    call("lsfusion_eval", "{\"script\":\"run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; }\",\"params\":[\"" + draft + "\"]," + first + "}", RAN));
            assertEquals("form | UiWebMCP.uiWebMCP | " + draft, call("lsfusion_eval", "{" + read + "," + first + ",\"context\":\"form\"}", RAN));
            assertEquals("form | UiWebMCP.uiWebMCPOther | " + saved, call("lsfusion_eval", "{" + read + "}", RAN));

            // the first form again, under a label: the name alone names two forms now, the label one
            open("UiWebMCP.uiWebMCPFolder", "UiWebMCP.uiWebMCPLabelled");
            Locator labelled = page.locator("[lsfusion-form='UiWebMCP.uiWebMCP'][lsfusion-form-label='labelled'][lsfusion-form-window='System.forms']");
            assertThat(labelled).isVisible();
            assertEquals("form | UiWebMCP.uiWebMCP | " + saved, call("lsfusion_eval", "{" + read + ",\"form\":{\"label\":\"labelled\"}}", RAN));
            assertEquals("form | UiWebMCP.uiWebMCP | " + saved,
                    call("lsfusion_eval", "{" + read + ",\"form\":{\"name\":\"UiWebMCP.uiWebMCP\",\"label\":\"labelled\",\"window\":\"System.forms\"}}", RAN));
            assertEquals("lsfusion_eval error: 2 open forms at the address {\"name\":\"UiWebMCP.uiWebMCP\"}", call("lsfusion_eval", "{" + read + "," + first + "}"));
            // ... and none: a form that is not open, a label no form has - the empty one too -, a window that is not there
            for (String none : new String[] {"{\"name\":\"UiWebMCP.uiWebMCPNoSuchForm\"}", "{\"name\":\"UiWebMCP.uiWebMCPOther\",\"label\":\"\"}",
                    "{\"name\":\"UiWebMCP.uiWebMCPOther\",\"window\":\"UiWebMCP.uiWebMCPNoSuchWindow\"}"})
                assertEquals("lsfusion_eval error: no open form at the address " + none, call("lsfusion_eval", "{" + read + ",\"form\":" + none + "}"));

            // what is no address, and an address beside another context, the server says
            String string = call("lsfusion_eval", "{" + read + ",\"form\":\"UiWebMCP.uiWebMCP\"}");
            assertTrue(string, string.startsWith("lsfusion_eval error: ") && string.contains("`form` must be an object"));
            String unknown = call("lsfusion_eval", "{" + read + ",\"form\":{\"id\":\"1\"}}");
            assertTrue(unknown, unknown.startsWith("lsfusion_eval error: ") && unknown.contains("unknown field `id`"));
            String navigator = call("lsfusion_eval", "{" + read + "," + first + ",\"context\":\"navigator\"}");
            assertTrue(navigator, navigator.startsWith("lsfusion_eval error: ") && navigator.contains("`context` is then `form`, or omitted"));
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

        assertEquals("navigator | 7", call("lsfusion_eval", "{\"script\":\"" + sum + "\",\"params\":[\"apples,3\\npears,4\\n\"]}", RAN));
        assertEquals("first=1,second=2", call("lsfusion_eval", marks, "answer => answer.results.map(entry => entry.name + '=' + entry.value).sort().join()"));
        String file = "answer => { const file = answer.results[0]; return [file.type, file.fileName, file.extension, file.mimeType, file.size, file.base64 || file.text].join(' '); }";
        assertEquals("file file.png png image/png 70 " + PNG, call("lsfusion_eval", png, file));
        assertEquals("file file.json json application/json 15 {\"a\":1,\"b\":\"x\"}", call("lsfusion_eval", json, file));
    }

    // what the page's run of the tool, in the navigator, answers, when the server's run of it answers the same
    private String answersAsTheServer(String arguments) {
        String onThePage = call("lsfusion_eval", arguments.replaceFirst("\\{", "{\"context\":\"navigator\","),
                "answer => { delete answer.context; return window.uiWebMCPText(answer); }");
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
        assertEquals("lsfusion_eval error: the user is in no form", call("lsfusion_eval", "{\"script\":\"run() { }\",\"context\":\"form\"}"));
        String unknown = call("lsfusion_eval", "{\"script\":\"run() { RETURN UiWebMCP.uiWebMCPNoSuchProperty(); }\"}");
        assertTrue(unknown, unknown.startsWith("lsfusion_eval error: ") && unknown.contains("uiWebMCPNoSuchProperty"));
        // what is no context, and what is no call at all, the server says
        String context = call("lsfusion_eval", "{\"script\":\"run() { }\",\"context\":\"server\"}");
        assertTrue(context, context.startsWith("lsfusion_eval error: ") && context.contains("`logics`, `navigator` or `form`"));
        String noArguments = call("lsfusion_eval", "null");
        assertTrue(noArguments, noArguments.startsWith("lsfusion_eval error: ") && noArguments.contains("'script' is required"));
        String missing = call("lsfusion_files_read", "{\"path\":\"/ui/UiWebMCPNoSuchFile.lsf\"}");
        assertTrue(missing, missing.startsWith("lsfusion_files_read error: "));
    }
}
