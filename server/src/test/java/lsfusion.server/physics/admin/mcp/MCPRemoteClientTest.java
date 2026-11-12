package lsfusion.server.physics.admin.mcp;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

// a tools/call of the central MCP server failed if its result has isError set, whatever its text: the MCP SDK the
// server runs on prefixes "Error executing tool" only to an error raised inside the tool, not to one raised before
// the tool runs
public class MCPRemoteClientTest {

    private static String response(String text, Boolean isError) {
        JSONObject result = new JSONObject()
                .put("content", new JSONArray().put(new JSONObject().put("type", "text").put("text", text)));
        if (isError != null)
            result.put("isError", isError);
        return new JSONObject().put("jsonrpc", "2.0").put("id", 1).put("result", result).toString();
    }

    // what the server answers for a tool it does not have, e.g. once the tool is removed or renamed there
    @Test
    public void errorRaisedBeforeToolRunsIsThrown() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> MCPRemoteClient.resultText(response("Unknown tool: lsfusion_retrieve_docs", true)));
        assertEquals("Unknown tool: lsfusion_retrieve_docs", e.getMessage());
    }

    @Test
    public void errorRaisedInToolIsThrown() {
        String text = "Error executing tool lsfusion_get_guidance: pass either rules or brief";
        RuntimeException e = assertThrows(RuntimeException.class, () -> MCPRemoteClient.resultText(response(text, true)));
        assertEquals(text, e.getMessage());
    }

    @Test
    public void successfulResultIsReturned() {
        assertEquals("{\"results\":[]}", MCPRemoteClient.resultText(response("{\"results\":[]}", false)));
        assertEquals("article", MCPRemoteClient.resultText(response("article", null)));
    }
}
