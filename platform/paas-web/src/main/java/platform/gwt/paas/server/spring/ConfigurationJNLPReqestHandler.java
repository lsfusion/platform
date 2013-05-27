package platform.gwt.paas.server.spring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.ClientJNLPRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static platform.base.BaseUtils.nvl;

public class ConfigurationJNLPReqestHandler extends ClientJNLPRequestHandler {
    protected final static Logger logger = Logger.getLogger(ConfigurationJNLPReqestHandler.class);

    private static final String CONFIGURATION_ID = "confId";

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> paasProvider;

    public void setPaasProvider(BusinessLogicsProvider<PaasRemoteInterface> paasProvider) {
        this.paasProvider = paasProvider;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Handling jnlp request");

        String confIdParam = request.getParameter(CONFIGURATION_ID);
        if (confIdParam == null) {
            throw new IllegalStateException("Configuration id isn't set!");
        }

        int confId;
        try {
            confId = Integer.parseInt(confIdParam);
        } catch (NumberFormatException nfe) {
            throw new IllegalStateException("Illegal configuration id!");
        }

        try {
            ConfigurationDTO configuration = paasProvider.getLogics().getConfiguration(null, confId);

            logger.debug("Read configuration: " + configuration.name + ":" + configuration.exportName);

            StringBuffer requestURL = request.getRequestURL();
            String codebaseUrl = requestURL.substring(0, requestURL.lastIndexOf("/"));
            String jnlpUrl = requestURL.append("?").append(CONFIGURATION_ID).append("=").append(confId).toString();
            String appName = nvl(configuration.name, "Launch app").trim();

            handleJNLPRequest(request, response, codebaseUrl, jnlpUrl, appName, request.getServerName(), paasProvider.getRegistryPort(), configuration.exportName);
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration can't be found.");
        }
    }
}
