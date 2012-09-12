package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import platform.base.ClassUtils;
import platform.client.navigator.ClientNavigatorElement;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static platform.base.BaseUtils.nvl;

public class BusinessLogicsProviderImpl<T extends RemoteLogicsInterface> implements BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(BusinessLogicsProviderImpl.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "7652";

    static {
        try {
            ClassUtils.initRMICompressedSocketFactory();
        } catch (IOException e) {
            logger.error("Ошибка при инициализации RMISocketFactory: ", e);
            throw new RuntimeException("Произошла ошибка при инициализации RMI.", e);
        }
    }

    public static RemoteLogicsInterface createRemoteLogics(ServletContext context) {
        try {
            String serverHost = nvl(context.getInitParameter("serverHost"), DEFAULT_HOST);
            String serverPort = nvl(context.getInitParameter("serverPort"), DEFAULT_PORT);

            Registry registry = LocateRegistry.getRegistry(serverHost, Integer.parseInt(serverPort));
            RemoteLoaderInterface loader = (RemoteLoaderInterface) registry.lookup("default/BusinessLogicsLoader");

            return loader.getRemoteLogics();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
        }
    }

    private ServletContext servletContext;

    private final Object remoteLock = new Object();

    private volatile T logics;

    public BusinessLogicsProviderImpl(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

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
            ClientNavigatorElement.dropCaches();
        }
    }
}
