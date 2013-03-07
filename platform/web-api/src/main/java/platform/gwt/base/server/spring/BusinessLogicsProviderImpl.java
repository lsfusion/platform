package platform.gwt.base.server.spring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import platform.base.SystemUtils;
import platform.gwt.base.server.ServerUtils;
import platform.interop.RemoteLogicsLoaderInterface;
import platform.interop.RemoteLogicsInterface;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static platform.base.BaseUtils.nvl;

public class BusinessLogicsProviderImpl<T extends RemoteLogicsInterface> implements BusinessLogicsProvider<T> {
    protected final static Logger logger = Logger.getLogger(BusinessLogicsProviderImpl.class);

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "7652";

    private final List<InvalidateListener> invlidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

    @Autowired
    private ServletContext servletContext;

    private volatile T logics;
    private final Object logicsLock = new Object();

    public T getLogics() {
        //double-check locking
        if (logics == null) {
            synchronized (logicsLock) {
                if (logics == null) {
                    logics = (T) BusinessLogicsProviderImpl.createRemoteLogics(servletContext);
                }
            }
        }
        return logics;
    }

    public void invalidate() {
        synchronized (logicsLock) {
            logics = null;

            for (InvalidateListener invalidateListener : invlidateListeners) {
                invalidateListener.onInvalidate();
            }
        }
    }

    @Override
    public void addInvlidateListener(InvalidateListener listener) {
        invlidateListeners.add(listener);
    }

    @Override
    public void removeInvlidateListener(InvalidateListener listener) {
        invlidateListeners.remove(listener);
    }

    public static RemoteLogicsInterface createRemoteLogics(ServletContext context) {
        try {
            String serverHost = nvl(context.getInitParameter("serverHost"), DEFAULT_HOST);
            String serverPort = nvl(context.getInitParameter("serverPort"), DEFAULT_PORT);

            Registry registry = LocateRegistry.getRegistry(serverHost, Integer.parseInt(serverPort));
            //todo: setup dbName
            RemoteLogicsLoaderInterface loader = (RemoteLogicsLoaderInterface) registry.lookup("default/RemoteLogicsLoader");

            ServerUtils.timeZone = loader.getLogics().getTimeZone();

            return loader.getLogics();
        } catch (Exception e) {
            logger.error("Ошибка при получении объекта логики: ", e);
            throw new RuntimeException("Произошла ошибка при подлючении к серверу приложения.", e);
        }
    }

    static {
        try {
            SystemUtils.initRMICompressedSocketFactory();
        } catch (IOException e) {
            logger.error("Ошибка при инициализации RMISocketFactory: ", e);
            throw new RuntimeException("Произошла ошибка при инициализации RMI.", e);
        }
    }

}
