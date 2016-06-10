package lsfusion.server.remote;

import com.google.common.io.Resources;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.server.ServerLoggers;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.RMIManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RemoteLogicsLoader extends LifecycleAdapter implements RemoteLogicsLoaderInterface, InitializingBean {
    private static final Logger logger = ServerLoggers.startLogger;

    public static final String EXPORT_NAME = "RemoteLogicsLoader";

    private RMIManager rmiManager;

    private RemoteLogics remoteLogics;

    private boolean started = false;

    public RemoteLogicsLoader() throws RemoteException {
        super(BLLOADER_ORDER);
    }

    public void setRmiManager(RMIManager rmiManager) {
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
            throw new RuntimeException("Port is already bound. Maybe another server is already running.");
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
            throw new RuntimeException(getString("logics.error.reading.class.on.the.server"), e);
        }
    }
}
