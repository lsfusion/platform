package lsfusion.interop.session;

import org.apache.hc.core5.http.ContentType;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

// the script an /eval request carries is not a parameter of the action it defines: a multipart part named script was
// taken for the first one as well, so $1 got the script text and every value moved one place further
public class EvalScriptParamTest {

    private static final String BOUNDARY = "lsfusionboundary";

    // what /eval got: the name of the param it took for the script, and the names of the params bound to $1, $2, ...
    private static class Eval implements ExecInterface {
        private String script;
        private String[] params;

        public ExternalResponse exec(String action, ExternalRequest request) {
            throw new UnsupportedOperationException();
        }

        public ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) {
            script = paramScript.name;
            params = Arrays.stream(request.params).filter(ExternalRequest.Param::isImplicitParam).map(param -> param.name).toArray(String[]::new);
            return null;
        }
    }

    // a multipart/form-data /eval/action request with the given query string and parts, as name, value, name, value, ...
    private static Eval evalAction(String query, String... parts) throws Exception {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < parts.length; i += 2)
            body.append("--").append(BOUNDARY).append("\r\nContent-Disposition: form-data; name=\"").append(parts[i]).append("\"\r\n\r\n").append(parts[i + 1]).append("\r\n");
        body.append("--").append(BOUNDARY).append("--\r\n");

        Eval eval = new Eval();
        ExternalUtils.processRequest(eval, request -> null, new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8)),
                ContentType.parse("multipart/form-data; boundary=" + BOUNDARY), new String[0], new String[0], new String[0], new String[0],
                null, null, null, "http", "POST", "localhost", 7651, "", "/eval", "/action", query, null);
        return eval;
    }

    @Test
    public void scriptPartIsNotAParam() throws Exception {
        Eval eval = evalAction("", "script", "RETURN $1;", "a", "x");
        assertEquals("script", eval.script);
        assertArrayEquals(new String[] {"a"}, eval.params);
    }

    @Test
    public void scriptPartAfterTheValuesIsNotAParam() throws Exception {
        Eval eval = evalAction("", "a", "x", "script", "RETURN $1;");
        assertEquals("script", eval.script);
        assertArrayEquals(new String[] {"a"}, eval.params);
    }

    @Test
    public void firstPartIsTheScriptWhenNoneIsNamedSo() throws Exception {
        Eval eval = evalAction("", "code", "RETURN $1;", "a", "x");
        assertEquals("code", eval.script);
        assertArrayEquals(new String[] {"a"}, eval.params);
    }

    @Test
    public void scriptInTheQueryLeavesThePartsParams() throws Exception {
        Eval eval = evalAction("script=RETURN%20%241%3B", "a", "x", "b", "y");
        assertEquals("script", eval.script);
        assertArrayEquals(new String[] {"a", "b"}, eval.params);
    }
}
