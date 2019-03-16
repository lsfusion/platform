package lsfusion.server.logics.controller.remote;

import com.google.common.io.Resources;
import lsfusion.interop.logics.RemoteLogicsInterface;
import lsfusion.interop.logics.RemoteLogicsLoaderInterface;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RemoteLogicsLoader extends LogicsManager implements RemoteLogicsLoaderInterface, InitializingBean {
    private static final Logger logger = ServerLoggers.startLogger;

    public static final String EXPORT_NAME = "RemoteLogicsLoader";

    private RmiManager rmiManager;

    private RemoteLogics remoteLogics;

    @Override
    protected BusinessLogics getBusinessLogics() {
        return remoteLogics.businessLogics;
    }

    private boolean started = false;

    public RemoteLogicsLoader() throws RemoteException {
        super(BLLOADER_ORDER);
    }

    public void setRmiManager(RmiManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setRemoteLogics(RemoteLogics remoteLogics) {
        this.remoteLogics = remoteLogics;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(remoteLogics, "remoteLogics must be specified");
        Assert.notNull(rmiManager, "rmiManager must be specified");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Binding Remote Logics Loader.");
        try {
            rmiManager.export(remoteLogics);
            rmiManager.bindAndExport(EXPORT_NAME, this);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException("Port (" + rmiManager.getPort() + ") is already bound. Maybe another server is already running.");
        } catch (Exception e) {
            throw new RuntimeException("Error binding Remote Logics Loader: ", e);
        }
        started = true;
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        if (started) {
            logger.info("Stopping Remote Logics Loader.");
            try {
                rmiManager.unexport(remoteLogics);
                rmiManager.unbindAndUnexport(EXPORT_NAME, this);
            } catch (Exception e) {
                throw new RuntimeException("Error stopping Remote Logics Loader: ", e);
            }
        }
    }

    public RemoteLogicsInterface getLogics() throws RemoteException {
        return remoteLogics;
    }

    public byte[] findClass(String name) throws RemoteException {
        try {
            return Resources.toByteArray(Resources.getResource(name.replace('.', '/') + ".class"));
        } catch (IOException e) {
            throw new RuntimeException(localize("{logics.error.reading.class.on.the.server}"), e);
        }
    }
}
