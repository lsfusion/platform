package lsfusion.tests;

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

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static lsfusion.tests.TestServer.BASE;

/** The MCP server's tools, on a server of its own. The web client's /mcp only carries a request's body to the
 *  server's RemoteLogicsInterface.mcp, which is what this calls. */
public class McpToolsIT {

    private static final String DATABASE = "lsfusion_test_mcp_" + ProcessHandle.current().pid();

    private static Process server;
    private static RemoteLogicsInterface logics;

    @BeforeClass
    public static void startServer() throws Exception {
        int[] ports = TestServer.freePorts(4);
        Path log = BASE.resolve("target/server-mcp.log");
        server = TestServer.start(DATABASE, ports, log, BASE.resolve("target/classes"),
                "settings.enableAPI=2"); // the calls here are anonymous
        TestServer.waitStarted(server, log);
        logics = RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", ports[1], "default", "RemoteLogicsLoader").getLogics();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null)
            TestServer.stop(server, DATABASE);
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

    // The server runs the script itself - `logics`, which an omitted context is here. Where the user works it is run by
    // the application's page, so such a call is refused here - after what is wrong with the call itself, which the page
    // sends here to be told
    @Test
    public void theEvalToolRunsItsScriptOnTheServer() throws Exception {
        Assert.assertEquals("yes 22", value(eval(EVAL_NUMBER, "[21, true]", "logics")));
        for (String context : new String[] {"navigator", "form"}) {
            JSONObject refused = eval(EVAL_NUMBER, "[21, true]", context);
            Assert.assertTrue(refused.toString(), refused.getBoolean("isError") && refused.toString().contains("only where the tool is called by the application's page"));
            JSONObject noCall = eval(EVAL_NUMBER, "\"none\"", context);
            Assert.assertTrue(noCall.toString(), noCall.getBoolean("isError") && noCall.toString().contains("`params` must be an array"));
        }
        JSONObject unknown = eval(EVAL_NUMBER, "[21, true]", "server");
        Assert.assertTrue(unknown.toString(), unknown.getBoolean("isError") && unknown.toString().contains("`logics`, `navigator` or `form`; got `server`"));
        Assert.assertEquals("[\"logics\",\"navigator\",\"form\"]",
                tool("lsfusion_eval").getJSONObject("inputSchema").getJSONObject("properties").getJSONObject("context").getJSONArray("enum").toString());
    }

    private static JSONObject eval(String script, String params) throws Exception {
        return eval(script, params, null);
    }

    // params is the text of the JSON array, as a client writes it: org.json would write 21.0 as 21
    private static JSONObject eval(String script, String params, String context) throws Exception {
        return answer(send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"lsfusion_eval\",\"arguments\":"
                + "{\"script\":" + JSONObject.quote(script) + ",\"params\":" + params + (context != null ? ",\"context\":" + JSONObject.quote(context) : "") + "}}}"));
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
        return new JSONObject(logics.mcp(AuthenticationToken.ANONYMOUS, new ConnectionInfo(new ComputerInfo("localhost", "127.0.0.1"), UserInfo.NULL),
                new ExternalRequest(new ExternalRequest.Param[0]), request).json);
    }
}
