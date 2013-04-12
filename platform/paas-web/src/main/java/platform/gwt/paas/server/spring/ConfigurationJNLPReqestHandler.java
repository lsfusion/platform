package platform.gwt.paas.server.spring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.HttpRequestHandler;
import paas.api.gwt.shared.dto.ConfigurationDTO;
import paas.api.remote.PaasRemoteInterface;
import platform.base.IOUtils;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.base.server.spring.BusinessLogicsProvider;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

import static platform.base.BaseUtils.nvl;

public class ConfigurationJNLPReqestHandler implements HttpRequestHandler {
    protected final static Logger logger = Logger.getLogger(ConfigurationJNLPReqestHandler.class);

    private static final String CONFIGURATION_ID = "confId";
    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private BusinessLogicsProvider<PaasRemoteInterface> blProvider;

    public void setBlProvider(BusinessLogicsProvider<PaasRemoteInterface> blProvider) {
        this.blProvider = blProvider;
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
            StringBuffer requestURL = request.getRequestURL();

            ConfigurationDTO configuration = blProvider.getLogics().getConfiguration(ServerUtils.getAuthentication().getName(), confId);
            logger.debug("Read configuration: " + configuration.name + ":" + configuration.port);

            Properties properties = new Properties();
            properties.put("codebase.url", requestURL.substring(0, requestURL.lastIndexOf("/")));
            properties.put("jnlp.url", requestURL.append("?").append(request.getQueryString()).toString());
            properties.put("scripted.name", configuration.name == null ? "Launch app" : configuration.name.trim());
            properties.put("scripted.host", getLogicsHost());
            properties.put("scripted.port", configuration.port.toString());

            String content = stringResolver.replacePlaceholders(
                    IOUtils.readStreamToString(getClass().getResourceAsStream("/client.jnlp")), properties
            );

            response.setContentType("application/x-java-jnlp-file");
            response.getOutputStream().write(content.getBytes());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration can't be found.");
        }
    }

    private String getLogicsHost() {
        return nvl(servletContext.getInitParameter("serverHost"), "localhost");
    }
}
