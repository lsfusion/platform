package lsfusion.gwt.base.server.spring;

import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReadMemoryLimitsRequestHandler implements HttpRequestHandler {

    @Autowired
    private LogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getParameter("path");
        Map<String, String> memoryLimits = new HashMap<>();
        try {
            memoryLimits = blProvider.getLogics().readMemoryLimits();
        } catch (Exception e) {
            blProvider.invalidate();
        }
        String url = "";
        for(Map.Entry<String, String> memoryLimit : memoryLimits.entrySet()) {
            url += String.format("<a href=\"%s/client.jnlp?%s\">Run desktop client %s</a><br/>", contextPath, memoryLimit.getValue(), memoryLimit.getKey());
        }
        response.getOutputStream().write(url.getBytes());
    }
}