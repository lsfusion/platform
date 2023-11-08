package lsfusion.server.logics.controller.remote;

import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.task.PublicTask;
import lsfusion.server.base.task.TaskRunner;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;

import static lsfusion.server.physics.admin.log.ServerLoggers.startLog;

public class RemoteLogicsLoader extends LogicsManager implements RemoteLogicsLoaderInterface, InitializingBean {
    public static final String EXPORT_NAME = "RemoteLogicsLoader";

    private RmiManager rmiManager;
    
    private DBManager dbManager;

    public DBManager getDbManager() {
        return dbManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

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

    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    private PublicTask initTask;

    public void setInitTask(PublicTask initTask) {
        this.initTask = initTask;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(remoteLogics, "remoteLogics must be specified");
        Assert.notNull(rmiManager, "rmiManager must be specified");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {

        try {
            new TaskRunner(getBusinessLogics()).runTask(initTask);
        } catch (Exception e) {
            throw new RuntimeException("Error starting ReflectionManager: ", e);
        }

        started = true;
    }

    public int getPort() {
        return rmiManager.getPort();
    }

    public void exportRmiObject() {
        try {
            rmiManager.export(remoteLogics);
            rmiManager.bindAndExport(EXPORT_NAME, this);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException("Port (" + rmiManager.getPort() + ") is already bound. Maybe another server is already running");
        } catch (Exception e) {
            throw new RuntimeException("Error binding Remote Logics Loader: ", e);
        }
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        if (started) {
            startLog("Stopping Remote Logics Loader");
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
}
