package lsfusion.gwt.base.server.spring;

import lsfusion.base.IOUtils;
import lsfusion.interop.VMOptions;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

import static lsfusion.base.BaseUtils.isRedundantString;

public class ClientJNLPRequestHandler implements HttpRequestHandler {
    protected final static Logger logger = Logger.getLogger(ClientJNLPRequestHandler.class);

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);
    private static final String DEFAULT_INIT_HEAP_SIZE = "32m";
    private static final String DEFAULT_MAX_HEAP_SIZE = "800m";

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Handling jnlp request");

        try {
            StringBuffer requestURL = request.getRequestURL();
            String codebaseUrl = requestURL.substring(0, requestURL.lastIndexOf("/"));
            VMOptions clientVMOptions;
            try {
                clientVMOptions = blProvider.getLogics().getClientVMOptions();
            } catch (Exception e) {
                //use default
                clientVMOptions = new VMOptions(null, null);
            }

            handleJNLPRequest(request, response, codebaseUrl, "client.jnlp", "lsFusion Client", request.getServerName(),
                              blProvider.getRegistryPort(), blProvider.getExportName(),
                              clientVMOptions.getInitHeapSize(), clientVMOptions.getMaxHeapSize());
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
                                     String registryHost,
                                     int registryPort,
                                     String exportName) throws ServletException, IOException {
        handleJNLPRequest(request, response, codebaseUrl, jnlpUrl, appName, registryHost, registryPort, exportName, null, null);
    }

    protected void handleJNLPRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String codebaseUrl,
                                     String jnlpUrl,
                                     String appName,
                                     String registryHost,
                                     int registryPort,
                                     String exportName,
                                     String initHeapSize,
                                     String maxHeapSize) throws ServletException, IOException {
        logger.debug("Generating jnlp response.");

        try {
            Properties properties = new Properties();
            properties.put("jnlp.codebase", codebaseUrl);
            properties.put("jnlp.url", jnlpUrl);
            properties.put("jnlp.appName", appName);
            properties.put("jnlp.registryHost", registryHost);
            properties.put("jnlp.registryPort", String.valueOf(registryPort));
            properties.put("jnlp.exportName", exportName);
            properties.put("jnlp.initHeapSize", !isRedundantString(initHeapSize) ? initHeapSize : DEFAULT_INIT_HEAP_SIZE);
            properties.put("jnlp.maxHeapSize", !isRedundantString(maxHeapSize) ? maxHeapSize : DEFAULT_MAX_HEAP_SIZE);

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
}
