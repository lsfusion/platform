package lsfusion.http;

import lsfusion.base.IOUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.VMOptions;
import org.apache.log4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static lsfusion.base.BaseUtils.isRedundantString;

public class ClientJNLPRequestHandler extends HttpLogicsRequestHandler {
    protected final static Logger logger = Logger.getLogger(ClientJNLPRequestHandler.class);

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);
    private static final String DEFAULT_INIT_HEAP_SIZE = "32m";
    private static final String DEFAULT_MAX_HEAP_SIZE = "800m";
    private static final String DEFAULT_MIN_HEAP_FREE_RATIO = "30";
    private static final String DEFAULT_MAX_HEAP_FREE_RATIO = "70";

    @Override
    protected void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("Handling jnlp request");

        try {
            StringBuffer requestURL = request.getRequestURL();
            String codebaseUrl = requestURL.substring(0, requestURL.lastIndexOf("/"));
            String queryString = request.getQueryString();
            String jnlpUrl = "client.jnlp" + (queryString == null || queryString.isEmpty() ? "" : ("?" + queryString));

            VMOptions clientVMOptions = remoteLogics.getClientVMOptions();
            String host = logicsConnection.host != null && !logicsConnection.host.equals("localhost") && !logicsConnection.host.equals("127.0.0.1")
                    ? logicsConnection.host : request.getServerName();
            handleJNLPRequest(request, response, codebaseUrl, jnlpUrl, "lsFusion", host,
                    logicsConnection.port, logicsConnection.exportName, remoteLogics.isSingleInstance(),
                    clientVMOptions.getInitHeapSize(), clientVMOptions.getMaxHeapSize(), clientVMOptions.getMinHeapFreeRatio(),
                    clientVMOptions.getMaxHeapFreeRatio(), clientVMOptions.getVmargs());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't generate jnlp.");
        }
    }

    protected void handleJNLPRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String codebaseUrl,
                                     String jnlpUrl,
                                     String appName,
                                     String host,
                                     int port,
                                     String exportName) throws IOException {
        handleJNLPRequest(request, response, codebaseUrl, jnlpUrl, appName, host, port, exportName, false, null, null, null, null, null);
    }

    protected void handleJNLPRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String codebaseUrl,
                                     String jnlpUrl,
                                     String appName,
                                     String host,
                                     int port,
                                     String exportName,
                                     boolean singleInstance,
                                     String initHeapSize,
                                     String maxHeapSize,
                                     String minHeapFreeRatio,
                                     String maxHeapFreeRatio,
                                     String vmargs) throws IOException {
        logger.debug("Generating jnlp response.");

        try {

            Map<String, String> queryParams = getQueryParams(request);
            String jnlpMaxHeapSize = queryParams.get("maxHeapSize");
            String jnlpVmargs = queryParams.get("vmargs");

            Properties properties = new Properties();
            properties.put("jnlp.codebase", codebaseUrl);
            properties.put("jnlp.url", jnlpUrl);
            properties.put("jnlp.appName", appName);
            properties.put("jnlp.host", host);
            properties.put("jnlp.port", String.valueOf(port));
            properties.put("jnlp.exportName", exportName);
            properties.put("jnlp.singleInstance", String.valueOf(singleInstance));
            properties.put("jnlp.initHeapSize", !isRedundantString(initHeapSize) ? initHeapSize : DEFAULT_INIT_HEAP_SIZE);
            properties.put("jnlp.maxHeapSize", jnlpMaxHeapSize != null ? jnlpMaxHeapSize : (!isRedundantString(maxHeapSize) ? maxHeapSize : DEFAULT_MAX_HEAP_SIZE));
            properties.put("jnlp.minHeapFreeRatio", !isRedundantString(minHeapFreeRatio) ? String.valueOf(minHeapFreeRatio) : DEFAULT_MIN_HEAP_FREE_RATIO);
            properties.put("jnlp.maxHeapFreeRatio", !isRedundantString(maxHeapFreeRatio) ? String.valueOf(maxHeapFreeRatio) : DEFAULT_MAX_HEAP_FREE_RATIO);
            properties.put("jnlp.vmargs", jnlpVmargs != null ? URLDecoder.decode(jnlpVmargs, "utf-8") : (!isRedundantString(vmargs) ? vmargs : ""));

            String content = stringResolver.replacePlaceholders(
                    IOUtils.readStreamToString(getClass().getResourceAsStream("/client.jnlp")), properties
            );

            response.setContentType("application/x-java-jnlp-file");
            response.getOutputStream().write(content.getBytes());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't generate jnlp.");
        }
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
