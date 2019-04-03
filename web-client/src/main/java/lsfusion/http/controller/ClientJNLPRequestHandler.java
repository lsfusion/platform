package lsfusion.http.controller;

import lsfusion.interop.logics.LogicsSessionObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.isRedundantString;

@Deprecated
public class ClientJNLPRequestHandler extends ExternalRequestHandler {
    protected final static Logger logger = Logger.getLogger(ClientJNLPRequestHandler.class);

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("Handling jnlp request");

        Map<String, String> queryParams = getQueryParams(request);
        String jnlpMaxHeapSize = queryParams.get("maxHeapSize");
        String jnlpVmargs = queryParams.get("vmargs");

        String path = "/exec?action=Security.generateJnlp%5BVARSTRING%5B10%5D,VARSTRING%5B1000%5D%5D";
        if (!isRedundantString(jnlpMaxHeapSize)) {
            path += "&p=" + jnlpMaxHeapSize;
        }
        if (!isRedundantString(jnlpVmargs)) {
            if (isRedundantString(jnlpMaxHeapSize)) {
                path += "&p=";
            }
            path += "&p=" + jnlpVmargs;
        }
        
        response.sendRedirect(request.getContextPath() + path);
    }

    private Map<String, String> getQueryParams(HttpServletRequest request) {
        final Map<String, String> queryParams = new HashMap<>();
        String queryString = request.getQueryString();
        if(queryString != null) {
            final String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                if (pair.contains("=")) {
                    final int idx = pair.indexOf("=");
                    queryParams.put(pair.substring(0, idx), pair.length() > idx + 1 ? pair.substring(idx + 1) : null);
                }
            }
        }
        return queryParams;
    }
}
