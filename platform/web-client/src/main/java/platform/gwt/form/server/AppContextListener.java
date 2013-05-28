package platform.gwt.form.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String appPath = servletContextEvent.getServletContext().getRealPath("");
        FileUtils.APP_FOLDER_URL = appPath;
        FileUtils.APP_IMAGES_FOLDER_URL = appPath + "/images/";
        FileUtils.APP_TEMP_FOLDER_URL = appPath + "/WEB-INF/temp";
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
