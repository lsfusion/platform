package lsfusion.server.base.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.remote.RMIUtils;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static lsfusion.server.physics.admin.log.ServerLoggers.startLog;
import static lsfusion.server.physics.admin.log.ServerLoggers.startLogError;

public class RmiManager extends LogicsManager implements InitializingBean {

    @Override
    protected BusinessLogics getBusinessLogics() {
        throw new UnsupportedOperationException();
    }
    
    private Registry registry;

    private String exportName;

    private int port = 0;

    private int httpPort = 0;

    private int webSocketPort = 0;

    private int debuggerPort = 0;

    private int jmxPort = 0;

    public RmiManager() {
        super(RMIMANAGER_ORDER);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getWebSocketPort() {
        return webSocketPort;
    }

    public void setWebSocketPort(int webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    public int getDebuggerPort() {
        return debuggerPort;
    }

    public void setDebuggerPort(int debuggerPort) {
        this.debuggerPort = debuggerPort;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.state(0 < port && port <= 65535, "port must be between 0 and 65535");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        startLog("Starting RMI manager");
        try {
            initJMX(); // важно, что до initRMI, так как должен использовать SocketFactory по умолчанию
            initRegistry();
        } catch (IOException e) {
            startLogError("Error starting RMI manager: ", e);
            Throwables.propagate(e);
        }
    }

    private void initJMX() {
        if(jmxPort == 0)
            return;

        // Ensure cryptographically strong random number generator used
        // to choose the object number - see java.rmi.server.ObjID
        //
//        System.setProperty("java.rmi.server.randomIDs", "true");

        try {
            final int port= jmxPort;
            //logger.info("Create RMI registry on port "+port);
            LocateRegistry.createRegistry(port);
    
            // Retrieve the PlatformMBeanServer.
            //logger.info("Get the platform's MBean server");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    
            // This where we would enable security - left out of this
            // for the sake of the example....
            //
    
            // Create an RMI connector server.
            //
            // As specified in the JMXServiceURL the RMIServer stub will be
            // registered in the RMI registry running in the local host on
            // port 3000 with the name "jmxrmi". This is the same name the
            // out-of-the-box management agent uses to register the RMIServer
            // stub too.
            //
            // The port specified in "service:jmx:rmi://"+hostname+":"+port
            // is the second port, where RMI connection objects will be exported.
            // Here we use the same port as that we choose for the RMI registry. 
            // The port for the RMI registry is specified in the second part
            // of the URL, in "rmi://"+hostname+":"+port
            //
            // logger.info("Create an RMI connector server");
            final String hostname = InetAddress.getLocalHost().getHostName();
            JMXServiceURL url =
                    new JMXServiceURL("service:jmx:rmi://"+hostname+
                            ":"+port+"/jndi/rmi://"+hostname+":"+port+"/jmxrmi");
    
            // Now create the server from the JMXServiceURL
            //
            JMXConnectorServer cs =
                    JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
    
            // Start the RMI connector server.
            //
            startLog("Start the RMI connector server on port " + port);
            cs.start();
        } catch (IOException e) {
            startLogError("Error starting JMX: ", e);
            Throwables.propagate(e);
        }
    }
    
    private void initRegistry() throws RemoteException {
        // first try external registry on that port
        // will disable that behaviour see RMIUtils.getRmiRegistry comment
//        registry = RMIUtils.getRmiRegistry(port);
//        try {
//            registry.list();
//        } catch (RemoteException e) {
        registry = RMIUtils.createRmiRegistry(port, ZipServerSocketFactory.getInstance());
//        }
    }

    private String getExportPath(String relativeName) {
        return exportName + "/" + relativeName;
    }

    public void export(Remote remote) throws RemoteException {
        RemoteObject.export(remote, port);
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
        bind(getExportPath(name), remote);
    }

    public void unbindAndUnexport(String name, Remote remote) throws RemoteException, NotBoundException {
        unbind(getExportPath(name));
        unexport(remote);
    }

    public String[] list() throws RemoteException {
        return registry.list();
    }
}
