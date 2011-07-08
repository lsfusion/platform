package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import org.springframework.web.context.ServletContextAware;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;
import platform.interop.remote.ServerSocketFactory;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;

import static platform.base.BaseUtils.nvl;

public class BusinessLogicsProviderImpl<T extends RemoteLogicsInterface> implements ServletContextAware, BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(BusinessLogicsProviderImpl.class);

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

    public static RemoteLogicsInterface createRemoteLogics(ServletContext context) {
        try {
            String serverHost = nvl(context.getInitParameter("serverHost"), DEFAULT_HOST);
            String serverPort = nvl(context.getInitParameter("serverPort"), DEFAULT_PORT);

            Registry registry = LocateRegistry.getRegistry(serverHost, Integer.parseInt(serverPort));
            RemoteLoaderInterface loader = (RemoteLoaderInterface) registry.lookup("BusinessLogicsLoader");

            return loader.getRemoteLogics();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.");
        }
    }

    private ServletContext servletContext;

    private BusinessLogicsProviderImpl() {
    }

    private final Object remoteLock = new Object();

    private volatile T logics;

    public T getLogics() {
        //double-check locking
        if (logics == null) {
            synchronized (remoteLock) {
                if (logics == null) {
                    logics = (T) BusinessLogicsProviderImpl.createRemoteLogics(servletContext);
                }
            }
        }
        return logics;
    }

    public void invalidate() {
        synchronized (remoteLock) {
            logics = null;
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;

    }
}
