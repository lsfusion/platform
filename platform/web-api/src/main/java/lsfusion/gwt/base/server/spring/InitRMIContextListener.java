package lsfusion.gwt.base.server.spring;

import org.apache.log4j.Logger;
import lsfusion.base.SystemUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;

public class InitRMIContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(InitRMIContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String registryHost = servletContextEvent.getServletContext().getInitParameter("registryHost");
        try {
            SystemUtils.initRMICompressedSocketFactory();
            SystemUtils.overrideRMIHostName(registryHost);
        } catch (IOException e) {
            logger.error("Ошибка при инициализации RMISocketFactory: ", e);
            throw new RuntimeException("Произошла ошибка при инициализации RMI.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {}
}
