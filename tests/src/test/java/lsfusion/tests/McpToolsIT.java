package lsfusion.tests;

import com.sun.net.httpserver.HttpServer;
import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.session.ExternalRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.tests.TestServer.BASE;

/** The MCP server's tools, on a server of its own. The web client's /mcp only carries a request's body to the
 *  server's RemoteLogicsInterface.mcp, which is what this calls. */
public class McpToolsIT {

    private static final String DATABASE = "lsfusion_test_mcp_" + ProcessHandle.current().pid();

    private static Process server;
    private static RemoteLogicsInterface logics;

    // a stand-in for the central MCP server (ai.lsfusion.org), which the server's tools lsfusion_get_guidance,
    // lsfusion_retrieve_docs and lsfusion_report_feedback are run by: it answers with the query of the request -
    // initialize with instructions and a session of its own, a tool with its text -, and keeps every request as
    // "<method> <query> <session>", the method a JSON-RPC one or DELETE, the end of a session
    private static HttpServer center;
    private static final List<String> centerRequests = new CopyOnWriteArrayList<>();
    private static final AtomicInteger centerSessions = new AtomicInteger();
    private static volatile boolean centerDown;

    @BeforeClass
    public static void startServer() throws Exception {
        center = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        center.createContext("/mcp", exchange -> {
            String method = exchange.getRequestMethod().equals("POST")
                    ? new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getString("method")
                    : exchange.getRequestMethod();
            String query = exchange.getRequestURI().getRawQuery();
            String session = exchange.getRequestHeaders().getFirst("mcp-session-id");
            JSONObject result = null;
            if (method.equals("initialize")) {
                session = "s" + centerSessions.incrementAndGet();
                exchange.getResponseHeaders().set("mcp-session-id", session);
                result = new JSONObject().put("protocolVersion", "2024-11-05").put("capabilities", new JSONObject())
                        .put("serverInfo", new JSONObject().put("name", "center").put("version", "0")).put("instructions", "instructions " + query);
            } else if (method.equals("tools/call"))
                result = new JSONObject().put("content", new JSONArray().put(new JSONObject().put("type", "text").put("text", "called " + query)));
            centerRequests.add(method + " " + query + " " + session);
            if (centerDown) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            byte[] body = new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("result", result).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        center.start();

        int[] ports = TestServer.freePorts(4);
        Path log = BASE.resolve("target/server-mcp.log");
        server = TestServer.start(DATABASE, ports, log, BASE.resolve("target/classes"),
                "settings.enableAPI=2", // the calls here are anonymous
                "lsfusion.server.mcpCenterUrl=http://localhost:" + center.getAddress().getPort() + "/mcp");
        TestServer.waitStarted(server, log);
        logics = RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", ports[1], "default", "RemoteLogicsLoader").getLogics();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
        if (center != null)
            center.stop(0);
    }

    // The center gives each path an agent reaches it by its own guidance, so the server names the path in every request
    // to the center: the page's WebMCP, which says so by `via=webapp` in the query of its request to /mcp, or else the
    // application's MCP. The session with the center is one for both, opened by the first call
    @Test
    public void theCenterIsToldThePathOfEveryRequest() throws Exception {
        centerRequests.clear();
        Assert.assertEquals("called via=webapp", remoteText("via=webapp"));
        Assert.assertEquals("called via=app", remoteText(null));
        Assert.assertEquals("called via=webapp", remoteText("port=7652&via=webapp")); // the page's own query stays before it
        Assert.assertEquals("called via=app", remoteText("via=ide")); // a path the server does not serve is the application's
        List<String> calls = new ArrayList<>(centerRequests);
        String session = calls.get(0).split(" ")[2];
        if (calls.get(0).startsWith("initialize ")) // the first call of the class's server opens the session
            Assert.assertEquals(centerRequests.toString(), "initialize via=webapp " + session, calls.remove(0));
        Assert.assertEquals(centerRequests.toString(), Arrays.asList("tools/call via=webapp " + session, "tools/call via=app " + session,
                "tools/call via=webapp " + session, "tools/call via=app " + session), calls);
    }

    // the server's initialize gives the client the center's instructions for its path, and ends the session it opened
    // for them; with no center to give them, none - the initialize does not fail
    @Test
    public void initializeGivesTheCentersInstructionsForThePath() throws Exception {
        for (String[] path : new String[][] {{null, "via=app"}, {"via=webapp", "via=webapp"}}) {
            centerRequests.clear();
            Assert.assertEquals("instructions " + path[1], initialize(path[0]).getString("instructions"));
            String session = centerRequests.get(0).split(" ")[2];
            Assert.assertEquals(Arrays.asList("initialize " + path[1] + " " + session, "DELETE " + path[1] + " " + session), centerRequests);
        }
        centerDown = true;
        try {
            JSONObject result = initialize(null);
            Assert.assertFalse(result.toString(), result.has("instructions"));
            Assert.assertEquals("lsfusion-server", result.getJSONObject("serverInfo").getString("name"));
        } finally {
            centerDown = false;
        }
    }

    private static JSONObject initialize(String query) throws Exception {
        return answer(send(new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "initialize")
                .put("params", new JSONObject().put("protocolVersion", "2025-06-18").put("capabilities", new JSONObject())
                        .put("clientInfo", new JSONObject().put("name", "test").put("version", "0"))).toString(), query));
    }

    // the text of lsfusion_get_guidance, which the center runs, called with `query` in the request to /mcp
    private static String remoteText(String query) throws Exception {
        JSONObject result = answer(send(new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
                .put("params", new JSONObject().put("name", "lsfusion_get_guidance").put("arguments", new JSONObject())).toString(), query));
        Assert.assertFalse(result.toString(), result.getBoolean("isError"));
        return result.getJSONArray("content").getJSONObject(0).getString("text");
    }

    // the eval tool's params are JSON values, each given to the class of its parameter as an http request gives a string
    private static final String EVAL_NUMBER = "run(INTEGER i, BOOLEAN b) { RETURN (IF b THEN 'yes ' ELSE 'no ') + STRING[10](i + 1); }";

    @Test
    public void theEvalToolTakesAJsonNumberAndABoolean() throws Exception {
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[21, true]")));
        Assert.assertEquals("no 22", value(eval(EVAL_NUMBER, "[21, false]")));
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[\"21\", \"true\"]"))); // a string still is taken

        String numeric = value(eval("run(NUMERIC[10,2] n) { RETURN n; }", "[1.25]"));
        Assert.assertEquals(numeric, 0, new BigDecimal(numeric).compareTo(new BigDecimal("1.25")));
        Assert.assertEquals("21", value(eval("run(STRING s) { RETURN s; }", "[21.0]"))); // and the string of it for a string
    }

    @Test
    public void anIntegerWrittenWithAFractionOrAnExponentIsThatInteger() throws Exception {
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[21.0, true]")));
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[2.1e1, true]")));
        Assert.assertEquals("yes 1001", value(eval(EVAL_NUMBER, "[1e3, true]")));
        Assert.assertEquals("yes 1", value(eval(EVAL_NUMBER, "[0.000, true]")));
        Assert.assertEquals("yes 1", value(eval(EVAL_NUMBER, "[0e19, true]")));
        Assert.assertEquals("9223372036854775807", value(eval("run(LONG l) { RETURN l; }", "[9223372036854775807.0]")));
        Assert.assertEquals("-0.0", value(eval("run(DOUBLE d) { RETURN d; }", "[-0.0]"))); // a DOUBLE has its own zeros

        // a number that is no integer is refused, not cut to 21
        JSONObject fraction = eval(EVAL_NUMBER, "[21.5, true]");
        Assert.assertTrue(fraction.toString(), fraction.getBoolean("isError") && fraction.toString().contains("21.5"));
        // and one of a billion digits is refused without writing them out
        JSONObject huge = eval(EVAL_NUMBER, "[1e999999999, true]");
        Assert.assertTrue(huge.toString(), huge.getBoolean("isError") && huge.toString().contains("1E+999999999"));
    }

    @Test
    public void theEvalToolTakesNullForAParameter() throws Exception {
        String script = "run(STRING a, INTEGER b, FILE c, STRING d) { RETURN (IF a THEN 'a' ELSE '-') + (IF b THEN 'b' ELSE '-') + (IF c THEN 'c' ELSE '-') + (IF d THEN 'd' ELSE '-'); }";
        Assert.assertEquals("a--d", value(eval(script, "[\"x\", null, null, \"y\"]"))); // the parameters after a null are not shifted
        Assert.assertEquals("-bc-", value(eval(script, "[null, 1, {\"text\": \"z\"}]")));
    }

    @Test
    public void aBooleanIsTheLogicalClassesValue() throws Exception {
        Assert.assertEquals("false", value(eval("run(TBOOLEAN t) { RETURN t; }", "[false]")));
        Assert.assertEquals("true", value(eval("run(TBOOLEAN t) { RETURN t; }", "[true]")));
        Assert.assertEquals("false", value(eval("run(STRING s) { RETURN s; }", "[false]"))); // its text for a string
    }

    @Test
    public void anObjectsIdIsANumber() throws Exception {
        String id = value(eval("run() { HttpUntypedParams.hupSeed(); RETURN (GROUP MAX HttpUntypedParams.HupOrder o IF HttpUntypedParams.hupNumber(o) = 'hup42'); }", "[]"));
        String number = "run(HttpUntypedParams.HupOrder o) { RETURN HttpUntypedParams.hupNumber(o); }";
        Assert.assertEquals("hup42", value(eval(number, "[" + id + "]")));
        Assert.assertEquals("hup42", value(eval(number, "[\"" + id + "\"]"))); // a string still is taken
    }

    private static final String EVAL_JSON = "run(JSON j) { RETURN STRING(j); }";
    private static final String EVAL_FILE = "run(FILE f) { RETURN f; }";

    @Test
    public void anArrayAndAnObjectAreJsonData() throws Exception {
        String array = value(eval(EVAL_JSON, "[[1, \"two\"]]"));
        Assert.assertEquals(array, "[1,\"two\"]", new JSONArray(array).toString());
        // an object is JSON data as an array is: it was refused as a file that has no content, so JSON data that is
        // an object had to be written as a string
        String object = value(eval(EVAL_JSON, "[{\"a\": 1, \"b\": {\"fileName\": \"no file\"}}]"));
        Assert.assertEquals(object, "1 no file", new JSONObject(object).getInt("a") + " " + new JSONObject(object).getJSONObject("b").getString("fileName"));
        Assert.assertEquals("{\"a\":1}", value(eval("run(STRING s) { RETURN s; }", "[{\"a\": 1}]"))); // the text of it for a string, as of an array
        Assert.assertEquals("{}", value(eval(EVAL_JSON, "[{}]")));
        String nested = value(eval(EVAL_JSON, "[{\"file\": {\"text\": \"x\"}}]")); // what is inside it decides nothing
        Assert.assertEquals(nested, "x", new JSONObject(nested).getJSONObject("file").getString("text"));
        // for a file parameter it is, as a string given for one is, the file of that text
        JSONObject file = result(eval(EVAL_FILE, "[{\"a\": 1}]"));
        Assert.assertEquals(file.toString(), "{\"a\":1} file", file.getString("text") + " " + file.getString("extension"));
        String string = value(eval(EVAL_JSON, "[\"{\\\"a\\\": 1}\"]")); // a string still is taken
        Assert.assertEquals(string, 1, new JSONObject(string).getInt("a"));

        // ... unless it describes a file - it has the file's content, or says it is a file: whatever its parameter is,
        // that is the file, so for a JSON parameter the text of the file is the JSON
        String text = value(eval(EVAL_JSON, "[{\"text\": \"{\\\"a\\\": 1}\"}]"));
        Assert.assertEquals(text, 1, new JSONObject(text).getInt("a"));
        JSONObject noContent = eval(EVAL_JSON, "[{\"type\": \"file\", \"fileName\": \"a.json\"}]");
        Assert.assertTrue(noContent.toString(), noContent.getBoolean("isError") && noContent.toString().contains("needs either `base64` or `text`"));
    }

    @Test
    public void anObjectIsAFile() throws Exception {
        JSONObject file = result(eval(EVAL_FILE, "[{\"text\": \"hello\", \"fileName\": \"h.txt\"}]"));
        Assert.assertEquals(file.toString(), "hello", file.getString("text"));
        Assert.assertEquals(file.toString(), "txt", file.getString("extension"));
        Assert.assertEquals(file.toString(), "text/plain", file.getString("mimeType")); // though the catalog of types has no txt

        // a string is the file's text, not its bytes in base64
        JSONObject csv = result(eval("run(CSVFILE f) { RETURN f; }", "[\"a;b\"]"));
        Assert.assertEquals(csv.toString(), "a;b", csv.getString("text"));
        Assert.assertEquals(csv.toString(), "csv", csv.getString("extension"));

        // the file is among the files of the request, as a number is among its params - the string of it
        JSONObject requestFile = result(eval("run() { RETURN fileParams('', 0); }", "[{\"text\": \"hello\", \"fileName\": \"h.txt\"}]"));
        Assert.assertEquals(requestFile.toString(), "hello", requestFile.getString("text"));
        Assert.assertEquals("21", value(eval("run() { RETURN params('', 0); }", "[21]")));
    }

    // the content of a file is named in a result as in a parameter - text, base64 -, so a file that came out of a call
    // is given to the next one as it came
    @Test
    public void aFileOfAResultIsAParameterAsItIs() throws Exception {
        String named = "run(NAMEDFILE f) { RETURN f; }";
        JSONObject text = result(eval(named, "[{\"text\": \"hello\", \"fileName\": \"report.v1.txt\"}]"));
        Assert.assertEquals(text.toString(), new HashSet<>(Arrays.asList("type", "fileName", "extension", "mimeType", "size", "text")), text.keySet());
        Assert.assertEquals(text.toString(), "report.v1.txt txt", text.getString("fileName") + " " + text.getString("extension")); // the name's own dot stays
        JSONObject twice = result(eval(named, "[{\"text\": \"x\", \"fileName\": \"report.txt.txt\"}]")); // ... and its own extension
        Assert.assertEquals(twice.toString(), "report.txt.txt", twice.getString("fileName"));
        Assert.assertTrue(twice.toString(), twice.similar(result(eval(named, "[" + twice + "]"))));
        Assert.assertTrue(text.toString(), text.similar(result(eval(named, "[" + text + "]"))));

        String bytes = "AAEC/w=="; // 0, 1, 2, 255 - no text
        JSONObject binary = result(eval(named, "[{\"base64\": \"" + bytes + "\", \"fileName\": \"b.bin\"}]"));
        Assert.assertEquals(binary.toString(), bytes, binary.getString("base64"));
        Assert.assertTrue(binary.toString(), binary.similar(result(eval(named, "[" + binary + "]"))));

        JSONObject kind = eval(EVAL_FILE, "[{\"type\": \"image\", \"text\": \"x\"}]");
        Assert.assertTrue(kind.toString(), kind.getBoolean("isError") && kind.toString().contains("type must be `file`"));
        // a file with a link in place of its content is no file to give
        JSONObject link = eval(EVAL_FILE, "[{\"type\": \"file\", \"extension\": \"pdf\", \"url\": \"/file/temp/mcp/x.pdf\"}]");
        Assert.assertTrue(link.toString(), link.getBoolean("isError") && link.toString().contains("unknown field `url`"));
    }

    // a file in JSON is the string of its bytes in base64, and the object a tool takes a file as too: an import reads both
    @Test
    public void aJsonImportReadsAFileGivenAsAnObject() throws Exception {
        String script = "run(JSONFILE j) { LOCAL f = NAMEDFILE (); IMPORT JSON FROM j TO() f; RETURN f(); }";
        JSONObject object = result(eval(script, "[" + JSONObject.quote("{\"f\": {\"text\": \"hello\", \"fileName\": \"h.txt\"}}") + "]"));
        Assert.assertEquals(object.toString(), "h.txt txt hello", object.getString("fileName") + " " + object.getString("extension") + " " + object.getString("text"));
        JSONObject base64 = result(eval(script, "[" + JSONObject.quote("{\"f\": \"aGVsbG8=\"}") + "]"));
        Assert.assertEquals(base64.toString(), 5, base64.getInt("size"));

        JSONObject unknown = eval(script, "[" + JSONObject.quote("{\"f\": {\"content\": \"hello\"}}") + "]");
        Assert.assertTrue(unknown.toString(), unknown.getBoolean("isError") && unknown.toString().contains("unknown field `content`"));
    }

    @Test
    public void theEvalToolsSchemaListsTheJsonValuesItTakes() throws Exception {
        JSONArray kinds = tool("lsfusion_eval").getJSONObject("inputSchema").getJSONObject("properties").getJSONObject("params")
                .getJSONObject("items").getJSONArray("oneOf");
        Set<String> types = new HashSet<>();
        for (int i = 0; i < kinds.length(); i++)
            types.add(kinds.getJSONObject(i).getString("type"));
        Assert.assertEquals(kinds.toString(), new HashSet<>(Arrays.asList("string", "number", "boolean", "array", "null", "object")), types);
        // an object is a file or JSON data, never both: the object that is JSON data is one that describes no file
        JSONObject data = kinds.getJSONObject(kinds.length() - 1);
        Assert.assertEquals(data.toString(), "[{\"required\":[\"text\"]},{\"required\":[\"base64\"]}]",
                new JSONArray().put(data.getJSONObject("not").getJSONArray("anyOf").get(0)).put(data.getJSONObject("not").getJSONArray("anyOf").get(1)).toString());
        Assert.assertEquals(data.toString(), "file", data.getJSONObject("not").getJSONArray("anyOf").getJSONObject(2).getJSONObject("properties").getJSONObject("type").getString("const"));
        Assert.assertTrue(kinds.toString(), kinds.getJSONObject(kinds.length() - 2).getJSONObject("properties").has("base64"));
    }

    // The server runs the script itself. Where the user works - in the navigator, or with `form` in a form - it is run
    // by the application's page: a `form` is refused here, whatever it is - the call is not run elsewhere -, and so is
    // the `context` the tool had; each after what is wrong with the call itself, which the page sends here to be told
    @Test
    public void theEvalToolRunsItsScriptOnTheServer() throws Exception {
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[21, true]", null)));
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[21, true]", "\"form\":null")));
        String change = "run(STRING[100] note) { UiWebMCP.uiWebMCPNote() <- note; APPLY; }", read = "run() { RETURN UiWebMCP.uiWebMCPNote(); }";
        eval(change, "[\"as it was\"]", null);
        for (String[] where : new String[][] {
                {"\"form\":{\"name\":\"A.b\"}", "`form` is a form of the application's page"},
                {"\"form\":{}", "`form` is a form of the application's page"},
                {"\"form\":\"A.b\"", "`form` is a form of the application's page"},
                {"\"context\":\"navigator\"", "`context` is no parameter of the tool"},
                {"\"context\":\"logics\"", "`context` is no parameter of the tool"}}) {
            JSONObject refused = eval(change, "[\"changed by a call that was refused\"]", where[0]);
            Assert.assertTrue(refused.toString(), refused.getBoolean("isError") && refused.toString().contains(where[1]));
            JSONObject noCall = eval(EVAL_NUMBER, "\"none\"", where[0]);
            Assert.assertTrue(noCall.toString(), noCall.getBoolean("isError") && noCall.toString().contains("`params` must be an array"));
        }
        Assert.assertEquals("as it was", value(eval(read, "[]", null))); // a call that is refused is not run
        JSONObject properties = tool("lsfusion_eval").getJSONObject("inputSchema").getJSONObject("properties");
        Assert.assertEquals(new HashSet<>(Arrays.asList("script", "params", "form")), properties.keySet());
        Assert.assertEquals(new HashSet<>(Arrays.asList("name", "label", "window")), properties.getJSONObject("form").getJSONObject("properties").keySet());
        Assert.assertFalse(properties.getJSONObject("form").has("minProperties")); // `{}` is the form the user is in
    }

    // The exec tool is the eval tool for an action that is in the logic: named, the action is run with the params and
    // answers as a script's does - on the server, as the script
    @Test
    public void theExecToolRunsAnActionOfTheLogicByItsName() throws Exception {
        Assert.assertEquals("42", value(exec("UiWebMCP.uiWebMCPDouble", "[21]", null)));
        Assert.assertEquals("42", value(exec("UiWebMCP.uiWebMCPDouble", "[\"21\"]", null)));
        for (String[] refused : new String[][] {
                {"UiWebMCP.uiWebMCPNoSuchAction", "[]", null, "uiWebMCPNoSuchAction"},
                {"", "[]", null, "'action' is required"},
                {"UiWebMCP.uiWebMCPDouble", "\"none\"", "\"form\":{}", "`params` must be an array"},
                {"UiWebMCP.uiWebMCPDouble", "[21]", "\"context\":\"navigator\"", "`context` is no parameter of the tool"},
                {"UiWebMCP.uiWebMCPDouble", "[21]", "\"form\":{\"name\":\"A.b\"}", "`form` is a form of the application's page"}}) {
            JSONObject answer = exec(refused[0], refused[1], refused[2]);
            Assert.assertTrue(answer.toString(), answer.getBoolean("isError") && answer.toString().contains(refused[3]));
        }
        // its descriptor is the eval tool's, with the action's name in place of the script
        JSONObject input = tool("lsfusion_exec").getJSONObject("inputSchema");
        Assert.assertEquals(new HashSet<>(Arrays.asList("action", "params", "form")), input.getJSONObject("properties").keySet());
        Assert.assertEquals("[\"action\"]", input.getJSONArray("required").toString());
        Assert.assertEquals(tool("lsfusion_eval").getJSONObject("outputSchema").toString(), tool("lsfusion_exec").getJSONObject("outputSchema").toString());
    }

    // the default glob was compiled to a regex that took seconds to fail on each of the classpath's ~80k paths, so a
    // search with the default arguments spent its whole default time limit choosing the files, and answered no hits
    // and timedOut - which an agent reads as "not found"
    @Test
    public void theFilesSearchToolFindsALineWithItsDefaults() throws Exception {
        JSONObject search = files("lsfusion_files_search", new JSONObject().put("regex", "^MODULE HttpUntypedParams;"));
        Assert.assertFalse(search.toString(), search.getBoolean("timedOut"));
        JSONArray hits = search.getJSONArray("hits");
        Assert.assertEquals(search.toString(), 1, hits.length());
        Assert.assertEquals("/integration/HttpUntypedParams.lsf", hits.getJSONObject(0).getString("path"));
    }

    @Test
    public void aDoubleStarSlashInAGlobIsWholeDirectories() throws Exception {
        Assert.assertEquals("[\"/integration/HttpUntypedParams.lsf\"]", list("**/integration/**/HttpUntypedParams.lsf"));
        Assert.assertEquals("[\"/integration/HttpUntypedParams.lsf\"]", list("/**/HttpUntypedParams.lsf"));
        Assert.assertEquals("[\"/integration/HttpUntypedParams.lsf\"]", list("**/**/integration/***/HttpUntypedParams.lsf"));
        // ** ended inside a name: this matched /integration/HttpUntypedParams.lsf
        Assert.assertEquals("[]", list("**/integration/**/Params.lsf"));
    }

    private static String list(String pathPattern) throws Exception {
        JSONObject list = files("lsfusion_files_list", new JSONObject().put("pathPattern", pathPattern));
        Assert.assertFalse(list.toString(), list.getBoolean("timedOut"));
        return list.getJSONArray("files").toString();
    }

    private static JSONObject files(String tool, JSONObject arguments) throws Exception {
        JSONObject result = answer(send(new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
                .put("params", new JSONObject().put("name", tool).put("arguments", arguments)).toString()));
        Assert.assertFalse(result.toString(), result.optBoolean("isError"));
        return result.getJSONObject("structuredContent");
    }

    private static JSONObject eval(String script, String params) throws Exception {
        return eval(script, params, null);
    }

    private static JSONObject eval(String script, String params, String where) throws Exception {
        return call("lsfusion_eval", "script", script, params, where);
    }

    private static JSONObject exec(String action, String params, String where) throws Exception {
        return call("lsfusion_exec", "action", action, params, where);
    }

    // a call of a tool that runs `run` - a script, an action by its name. params is the text of the JSON array, as a
    // client writes it: org.json would write 21.0 as 21; where - the arguments that say where it runs, as the text of
    // their fields, null for none
    private static JSONObject call(String tool, String run, String value, String params, String where) throws Exception {
        return answer(send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool + "\",\"arguments\":"
                + "{\"" + run + "\":" + JSONObject.quote(value) + ",\"params\":" + params + (where != null ? "," + where : "") + "}}}"));
    }

    private static String value(JSONObject result) {
        return result(result).getString("value");
    }

    // the one result of a call
    private static JSONObject result(JSONObject result) {
        Assert.assertFalse(result.toString(), result.getBoolean("isError"));
        return result.getJSONObject("structuredContent").getJSONArray("results").getJSONObject(0);
    }

    private static JSONObject tool(String name) throws Exception {
        JSONArray tools = answer(send(new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/list").toString())).getJSONArray("tools");
        for (int i = 0; i < tools.length(); i++)
            if (tools.getJSONObject(i).getString("name").equals(name))
                return tools.getJSONObject(i);
        return null;
    }

    private static JSONObject answer(JSONObject response) {
        Assert.assertTrue(response.toString(), response.has("result"));
        return response.getJSONObject("result");
    }

    private static JSONObject send(String request) throws Exception {
        return send(request, null);
    }

    // `query` - of the request to /mcp, as the web client passes it on
    private static JSONObject send(String request, String query) throws Exception {
        return new JSONObject(logics.mcp(AuthenticationToken.ANONYMOUS, new ConnectionInfo(new ComputerInfo("localhost", "127.0.0.1"), UserInfo.NULL),
                new ExternalRequest(new ExternalRequest.Param[0], null, null, null, null, null, null, null, null, null, null, null, query, null, null), request).json);
    }
}
