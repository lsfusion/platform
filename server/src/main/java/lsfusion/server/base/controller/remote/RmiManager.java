package lsfusion.server.base.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.base.file.StringWithFiles;
import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.logics.remote.RemoteClientInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.session.SessionInfo;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.logics.BusinessLogics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

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

    private boolean https = false;

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

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
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
        registry = RMIUtils.createRmiRegistry(port);
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

    private final Object syncRemoteClients = new Object();
    public List<RemoteClientInterface> remoteClients = new ArrayList<>();

    public void registerClient(RemoteClientInterface remoteClient) {
        synchronized (syncRemoteClients) {
            remoteClients.add(remoteClient);
        }
    }

    public interface RemoteFunction<T> {
        T apply(RemoteClientInterface remoteClient) throws RemoteException;
    }
    public <T> T executeOnSomeClient(RemoteFunction<T> func) {
        RemoteClientInterface remoteClient;
        synchronized (syncRemoteClients) {
            if(remoteClients.isEmpty())
                throw new RuntimeException("No web client was found to save the images");

            remoteClient = remoteClients.get(0);
        }

        try {
            return func.apply(remoteClient);
        } catch (RemoteException e) {
            synchronized (syncRemoteClients) {
                remoteClients.remove(remoteClient);
            }
            return executeOnSomeClient(func);
        }
    }

    private final static LRUWSVSMap<RemoteClientInterface, Serializable, String> cachedConversions = new LRUWSVSMap<>(LRUUtil.G3);
    private final static ConnectionInfo connectionInfo = new ConnectionInfo(new ComputerInfo(SystemUtils.getLocalHostName(), SystemUtils.getLocalHostIP()), UserInfo.NULL);

    public Object convertFileValue(ExternalRequest request, Object value) {
        if (value instanceof StringWithFiles) {
            StringWithFiles stringWithFiles = (StringWithFiles) value;
            return executeOnSomeClient(remoteClient -> {
                Serializable[] files = stringWithFiles.files;
                String[] convertedFiles = new String[files.length];

                List<Serializable> readFiles = new ArrayList<>();
                List<Integer> readIndices = new ArrayList<>();

                for (int i = 0, filesLength = files.length; i < filesLength; i++) {
                    Serializable file = files[i];
                    String cachedConversion = cachedConversions.get(remoteClient, file);
                    if (cachedConversion != null)
                        convertedFiles[i] = cachedConversion.equals(AppServerImage.NULL) ? null : cachedConversion;
                    else {
                        readFiles.add(file);
                        readIndices.add(i);
                    }
                }

                if (!readFiles.isEmpty()) { // optimization
                    String[] remoteConvertedFiles = remoteClient.convertFileValue(new SessionInfo(connectionInfo, request), readFiles.toArray(new Serializable[0]));
                    for (int i = 0; i < remoteConvertedFiles.length; i++) {
                        String convertedFile = remoteConvertedFiles[i];
                        cachedConversions.put(remoteClient, readFiles.get(i), convertedFile == null ? AppServerImage.NULL : convertedFile);
                        convertedFiles[readIndices.get(i)] = convertedFile;
                    }
                }

                return ExternalUtils.convertFileValue(stringWithFiles.prefixes, convertedFiles);
            });
        }
        return value;
    }
}
