package lsfusion.http;

import lsfusion.gwt.server.form.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class ReadMemoryLimitsRequestHandler extends HttpLogicsRequestHandler {

    @Override
    protected void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getParameter("path");
        Map<String, String> memoryLimits = remoteLogics.readMemoryLimits();
        String url = "";
        for(Map.Entry<String, String> memoryLimit : memoryLimits.entrySet()) {
            url += String.format("<a href=\"%s/client.jnlp?%s\">Run desktop client %s</a><br/>", contextPath, memoryLimit.getValue(), memoryLimit.getKey());
        }
        response.getOutputStream().write(url.getBytes());
    }
}