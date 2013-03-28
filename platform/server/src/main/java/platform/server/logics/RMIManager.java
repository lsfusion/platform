package platform.server.logics;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.base.SystemUtils;
import platform.server.ServerLoggers;
import platform.server.SystemProperties;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;

import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIManager extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;

    private Registry registry;

    //todo: переименовать во что-то более логичное типа namespace...
    private String dbName;

    private int exportPort;

    public RMIManager() {
        super(RMIMANAGER_ORDER);
    }

    public int getExportPort() {
        return exportPort;
    }

    public void setExportPort(int exportPort) {
        this.exportPort = exportPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(0 < exportPort && exportPort <= 65535, "exporPort must be between 0 and 65535");
        if (dbName == null) {
            dbName = "default";
        }
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Starting RMI Manager.");
        try {
            initRMI();
            initRegistry();
        } catch (RemoteException e) {
            throw new RuntimeException("Error starting RMIManager: ", e);
        }
    }

    private void initRMI() {
        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов
        SystemProperties.setGCIntervalIfNotDefined("600000");

//        if (!SystemProperties.isDebug) {
//            SystemProperties.setDGCLeaseValue();
//        }

        try {
            SystemUtils.initRMICompressedSocketFactory();
        } catch (IOException e) {
            logger.error("Error starting RMIManager: ", e);
            Throwables.propagate(e);
        }
    }

    private void initRegistry() throws RemoteException {
        //сначала ищем внешний registry на этом порту
        registry = LocateRegistry.getRegistry(exportPort);
        try {
            //данный вызов позволяет убедиться, что registry найден
            registry.list();
        } catch (RemoteException e) {
            //если не найден - создаём локальнй registry
            registry = LocateRegistry.createRegistry(exportPort);
        }
    }

    public void export(Remote remote) throws RemoteException {
        UnicastRemoteObject.exportObject(remote, exportPort);
    }

    public void unexport(Remote remote) throws RemoteException {
        UnicastRemoteObject.unexportObject(remote, true);
    }

    public void bind(String name, Remote remote) throws RemoteException, AlreadyBoundException {
        registry.bind(name, remote);
    }

    public void unbind(String name) throws RemoteException, NotBoundException {
        registry.unbind(name);
    }

    public void bindAndExport(String name, Remote remote) throws RemoteException, AlreadyBoundException {
        export(remote);
        bind(name, remote);
    }

    public void unbindAndUnexport(String name, Remote remote) throws RemoteException, AlreadyBoundException, NotBoundException {
        unbind(name);
        unexport(remote);
    }
}
