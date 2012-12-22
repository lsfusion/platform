package paas.manager.server;

import org.apache.commons.exec.*;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import paas.PaasUtils;
import paas.manager.common.ConfigurationEventData;
import platform.base.NullOutputStream;
import platform.base.OrderedMap;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.remote.ApplicationTerminal;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.DataObject;
import platform.server.logics.scripted.ScriptingBusinessLogics;
import platform.server.session.DataSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static platform.base.BaseUtils.nvl;

public final class AppManager {
    private final static Logger logger = Logger.getLogger(AppManager.class);

    private static final String javaExe = System.getProperty("java.home") + "/bin/java";

    private final ManagedAppLifecycleListener lifecycleListener = new ManagedAppLifecycleListener();

    private final ChannelGroup openChannels = new DefaultChannelGroup();

    private ChannelFactory channelFactory;

    private boolean started = false;

    private final int acceptPort;
    private PaasBusinessLogics paas;
    private PaasLogicsModule paasLM;

    public AppManager(int acceptPort) {
        this.acceptPort = acceptPort;
    }

    public void start() {
        if (started) {
            return;
        }
        channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(paas)));

        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectDecoder(),
                        new AppManagerChannelHandler(AppManager.this)
                );
            }
        });

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        Channel serverChannel = bootstrap.bind(new InetSocketAddress(acceptPort));

        openChannels.add(serverChannel);

        started = true;
    }

    public void stop() {
        if (!started) {
            return;
        }

        ChannelGroupFuture future = openChannels.close();
        future.awaitUninterruptibly();
        channelFactory.releaseExternalResources();
        started = false;
    }

    public void addOpenedChannel(Channel channel) {
        openChannels.add(channel);
    }

    public void lifecycleEvent(LifecycleEvent event) {
        lifecycleListener.lifecycleEvent(event);
    }

    public void setLogics(PaasBusinessLogics logics) {
        paas = logics;
        paasLM = paas.paasLM;
    }

    public String getStatus(int port) {
        try {
            ApplicationTerminal remoteManager = (ApplicationTerminal) Naming.lookup("rmi://localhost:" + port + "/AppTerminal");
            return "started";
        } catch (Exception e) {
            return isPortAvailable(port) ? "stopped" : "busyPort";
        }
    }

    public boolean isPortAvailable(int port) {
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);
        } catch (Exception e) {
            // Getting exception means the port is not used by other applications
            return true;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return false;
    }

    public void stopApplication(Integer port) throws RemoteException, MalformedURLException, NotBoundException {
        ApplicationTerminal remoteManager = (ApplicationTerminal) Naming.lookup("rmi://localhost:" + port + "/AppTerminal");
        remoteManager.stop();
    }

    public void executeScriptedBL(DataSession session, DataObject confObj) throws IOException, InterruptedException, SQLException {
        Integer port = (Integer) paasLM.configurationPort.read(session, confObj);

        PaasUtils.checkPortExceptionally(port);

        if (!paas.appManager.isPortAvailable(port)) {
            throw new IllegalStateException("Port is busy.");
        }

        String dbName = (String) paasLM.configurationDatabaseName.read(session, confObj);
        if (dbName == null) {
            throw new IllegalStateException("DB name is empty.");
        }

        Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);

        ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("moduleKey"));
        Expr moduleExpr = keys.get("moduleKey");
        Expr projExpr = new DataObject(projId, paasLM.project).getExpr();

        QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
        q.and(
                paasLM.moduleInProject.getExpr(session.getModifier(), projExpr, moduleExpr).getWhere()
        );
        q.addProperty("moduleOrder", paasLM.moduleOrder.getExpr(session.getModifier(), projExpr, moduleExpr));
        q.addProperty("moduleSource", paasLM.moduleSource.getExpr(session.getModifier(), moduleExpr));

        ImOrderMap<String, Boolean> orders = MapFact.singletonOrder("moduleOrder", false);
        ImOrderMap<ImMap<String,Object>, ImMap<String, Object>> values = q.execute(session.sql, orders);

        List<String> moduleFilePaths = new ArrayList<String>();
        for (ImMap<String, Object> entry : values.valueIt()) {
            String moduleSource = nvl((String) entry.get("moduleSource"), "");
            moduleFilePaths.add(createTemporaryScriptFile(moduleSource));
        }

        executeScriptedBL((Integer) confObj.object, port, dbName, moduleFilePaths);
    }


    public void executeScriptedBL(int confId, int port, String dbName, List<String> scriptFilePaths) throws IOException, InterruptedException {
        CommandLine commandLine = new CommandLine(javaExe);
        commandLine.addArgument("-Dlsf.settings.path=conf/scripted/settings.xml");
        commandLine.addArgument("-Dpaas.manager.conf.id=" + confId);
        commandLine.addArgument("-Dpaas.manager.host=localhost");
        commandLine.addArgument("-Dpaas.manager.port=" + acceptPort);
        commandLine.addArgument("-Dpaas.scripted.port=" + port);
        commandLine.addArgument("-Dpaas.scripted.db.name=" + dbName);
        commandLine.addArgument("-Dpaas.scripted.modules.paths=" + toParameters(scriptFilePaths), false);
        String rmiServerHostname = System.getProperty("java.rmi.server.hostname");
        if (rmiServerHostname != null) {
            commandLine.addArgument("-Djava.rmi.server.hostname=" + rmiServerHostname);
        }

        commandLine.addArgument("-cp");
        commandLine.addArgument(System.getProperty("java.class.path"));
        commandLine.addArgument(ScriptingBusinessLogics.class.getName());

        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(new NullOutputStream(), new NullOutputStream()));
//        executor.setStreamHandler(new PumpStreamHandler());
        executor.setExitValue(0);

        executor.execute(commandLine, new ManagedLogicsExecutionHandler(confId));
    }

    private String createTemporaryScriptFile(String moduleSource) throws IOException {
        File moduleFile = File.createTempFile("paas", ".lsf");

        PrintStream ps = new PrintStream(new FileOutputStream(moduleFile), false, "UTF-8");
        ps.print(moduleSource);
        ps.close();

        return moduleFile.getAbsolutePath();
    }

    private class ManagedLogicsExecutionHandler extends DefaultExecuteResultHandler {
        private final int configurationId;

        public ManagedLogicsExecutionHandler(int configurationId) {
            this.configurationId = configurationId;
        }

        @Override
        public void onProcessFailed(ExecuteException e) {
            super.onProcessFailed(e);

            logger.error("Error executing process: " + e.getMessage(), e.getCause());
            lifecycleListener.onError(
                    new LifecycleEvent(
                            LifecycleEvent.ERROR,
                            new ConfigurationEventData(configurationId, "Error while executing the process: " + e.getMessage())
                    )
            );
        }
    }

    private String toParameters(List<String> strings) {
        StringBuilder result = new StringBuilder(strings.size() * 30);
        for (String string : strings) {
            if (result.length() != 0) {
                result.append(";");
            }
            result.append(string);
        }

        return result.toString();
    }

    public class ManagedAppLifecycleListener extends LifecycleAdapter {
        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            super.lifecycleEvent(event);
            logger.debug("Lifecycle event: " + event);
        }

        private int getConfigurationId(LifecycleEvent event) {
            return ((ConfigurationEventData) event.getData()).configurationId;
        }

        private Object getEventData(LifecycleEvent event) {
            return ((ConfigurationEventData) event.getData()).data;
        }

        @Override
        protected void onStarted(LifecycleEvent event) {
            paas.changeConfigurationStatus(getConfigurationId(event), "started");
        }

        @Override
        protected void onStopped(LifecycleEvent event) {
            paas.changeConfigurationStatus(getConfigurationId(event), "stopped");
        }

        @Override
        protected void onError(LifecycleEvent event) {
            String errorMsg = (String) getEventData(event);

            paas.pushConfigurationLaunchError(getConfigurationId(event), errorMsg);
            logger.error("Error on managed app: " + errorMsg);
        }

        @Override
        protected void onOtherEvent(LifecycleEvent event) {
            logger.warn("Unrecognized lifecycle event was received from managed application.");
        }
    }
}
