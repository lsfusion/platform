package platform.gwt.base.server;

import org.apache.log4j.Logger;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;
import platform.interop.remote.ServerSocketFactory;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;

import static platform.base.BaseUtils.nvl;

public class GwtLogicsProvider {
    protected final static Logger logger = Logger.getLogger(GwtLogicsProvider.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "7652";

    static {
        try {
            if (RMISocketFactory.getSocketFactory() == null) {
                RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
                    public boolean failure(Exception ex) {
                        logger.error("Ошибка RMI: ", ex);
                        return true;
                    }
                });

                RMISocketFactory.setSocketFactory(new ServerSocketFactory());
            }
        } catch (IOException e) {
            logger.error("Ошибка при инициализации RMISocketFactory: ", e);
            throw new RuntimeException("Произошла ошибка при инициализации RMI.");
        }
    }

    public static RemoteLogicsInterface getLogics(ServletContext context) {
        try {
            String serverHost = nvl(context.getInitParameter("serverHost"), DEFAULT_HOST);
            String serverPort = nvl(context.getInitParameter("serverPort"), DEFAULT_PORT);
            RemoteLoaderInterface loader = (RemoteLoaderInterface) Naming.lookup("rmi://" + serverHost + ":" + serverPort + "/BusinessLogicsLoader");
            return loader.getRemoteLogics();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.");
        }
    }
}
