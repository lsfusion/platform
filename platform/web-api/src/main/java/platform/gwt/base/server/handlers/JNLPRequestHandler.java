package platform.gwt.base.server.handlers;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.HttpRequestHandler;
import platform.base.BaseUtils;
import platform.base.IOUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class JNLPRequestHandler implements HttpRequestHandler {
    protected final static Logger logger = Logger.getLogger(JNLPRequestHandler.class);

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);

    @Autowired
    private ServletContext servletContext;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Handling jnlp request");

        try {
            StringBuffer requestURL = request.getRequestURL();
            Properties properties = new Properties();
            properties.put("codebase.url", requestURL.substring(0, requestURL.lastIndexOf("/")));
            properties.put("client.host", request.getServerName());
            properties.put("client.port", BaseUtils.nvl(servletContext.getInitParameter("serverPort"), "7652"));

            String content = stringResolver.replacePlaceholders(
                    IOUtils.readStreamToString(new FileInputStream(servletContext.getRealPath("client.jnlp"))), properties
            );

            response.setContentType("application/x-java-jnlp-file");
            response.getOutputStream().write(content.getBytes());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Configuration can't be found.");
        }
    }
}
