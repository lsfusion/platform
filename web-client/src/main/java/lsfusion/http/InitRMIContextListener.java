package lsfusion.http;

import lsfusion.interop.remote.RMIUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;

public class InitRMIContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(InitRMIContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String host = servletContextEvent.getServletContext().getInitParameter("host");
        try {
            RMIUtils.initRMI();
            RMIUtils.overrideRMIHostName(host);
        } catch (IOException e) {
            logger.error("Ошибка при инициализации RMISocketFactory: ", e);
            throw new RuntimeException("Произошла ошибка при инициализации RMI.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {}
}
