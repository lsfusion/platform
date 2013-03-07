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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import paas.PaasBusinessLogics;
import paas.PaasLogicsModule;
import paas.PaasUtils;
import paas.manager.common.NotificationData;
import platform.base.IOUtils;
import platform.base.NullOutputStream;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import paas.terminal.ApplicationTerminal;
import platform.server.context.Context;
import platform.server.context.ContextAwareDaemonThreadFactory;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.BusinessLogicsBootstrap;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsInstance;
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
import java.util.Properties;
import java.util.concurrent.Executors;

import static platform.base.BaseUtils.nvl;
import static platform.server.lifecycle.LifecycleEvent.*;

public final class AppManager extends LifecycleAdapter implements InitializingBean {
    private final static Logger logger = Logger.getLogger(AppManager.class);

    private static final String javaExe = System.getProperty("java.home") + "/bin/java";

    private final ChannelGroup openChannels = new DefaultChannelGroup();

    private ChannelFactory channelFactory;

    private boolean started = false;

    private int acceptPort;

    private Context instanceContext;

    private PaasBusinessLogics paas;

    private PaasLogicsModule paasLM;

    private String dbServer;
    private String dbUser;
    private String dbPassword;

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        instanceContext = logicsInstance.getContext();
    }

    public void setBusinessLogics(PaasBusinessLogics businessLogics) {
        this.paas = businessLogics;
    }

    public void setAcceptPort(int acceptPort) {
        this.acceptPort = acceptPort;
    }

    public void setDbServer(String dbServer) {
        this.dbServer = dbServer;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(paas, "businessLogics must be specified");
        Assert.notNull(dbServer, "dbServer must be specified");
        Assert.notNull(dbUser, "dbUser must be specified");
        Assert.notNull(dbPassword, "dbPassword must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(instanceContext, "logicsInstance must be specified");

        Assert.state(0 < acceptPort && acceptPort <= 65535, "acceptPort must be between 0 and 65535");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Starting PAAS AppManager.");
        paasLM = paas.paasLM;
        start();
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        logger.info("Stopping PAAS AppManager.");
        stop();
    }

    public void start() {
        if (started) {
            return;
        }
        channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(instanceContext)));

        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ObjectDecoder(),
                        new AppManagerReceiveDataHandler(AppManager.this)
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

    public String getStatus(int port) {
        try {
            getManagedAppTerminal(port);
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
        getManagedAppTerminal(port).stop();
    }

    private ApplicationTerminal getManagedAppTerminal(int port) throws NotBoundException, MalformedURLException, RemoteException {
        return (ApplicationTerminal) Naming.lookup("rmi://localhost:" + port + "/default/AppTerminal");
    }

    public void executeConfiguration(DataSession session, DataObject confObj) throws IOException, InterruptedException, SQLException {
        Integer exportPort = (Integer) paasLM.configurationPort.read(session, confObj);

        PaasUtils.checkPortExceptionally(exportPort);

        if (!isPortAvailable(exportPort)) {
            throw new IllegalStateException("Port is busy.");
        }

        String dbName = (String) paasLM.configurationDatabaseName.read(session, confObj);
        if (dbName == null) {
            throw new IllegalStateException("DB name is empty.");
        }

        dbName = dbName.trim();

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

        //подготавливаем файлы для заупска
        File tempProjectDir = IOUtils.createTempDirectory("paas-project");
        File tempModulesDir = new File(tempProjectDir, "paasmodules");
        tempModulesDir.mkdir();

        List<String> moduleFilePaths = new ArrayList<String>();
        for (ImMap<String, Object> entry : values.valueIt()) {
            String moduleSource = nvl((String) entry.get("moduleSource"), "");
            moduleFilePaths.add(createTemporaryScriptFile(tempModulesDir, moduleSource));
        }

        executeScriptedBL((Integer) confObj.object, exportPort, dbName, tempProjectDir, moduleFilePaths);
    }

    private void executeScriptedBL(int configurationId, int exportPort, String dbName, File tempProjectDir, List<String> scriptFilePaths) throws IOException, InterruptedException {
        Properties properties = new Properties();
        properties.setProperty("db.server", dbServer);
        properties.setProperty("db.name", dbName);
        properties.setProperty("db.user", dbUser);
        properties.setProperty("db.password", dbPassword);
        properties.setProperty("rmi.exportPort", String.valueOf(exportPort));
        properties.setProperty("logics.overridingModulesList", "");
        properties.setProperty("logics.includedPaths", "paasmodules");
        properties.setProperty("logics.exludedPaths", "");
        properties.setProperty("paas.manager.host", "localhost");
        properties.setProperty("paas.manager.port", String.valueOf(acceptPort));
        properties.setProperty("paas.configurationId", String.valueOf(configurationId));
        FileOutputStream out = new FileOutputStream(new File(tempProjectDir, "settings.properties"));
        properties.store(out, null);
        out.close();

        CommandLine commandLine = new CommandLine(javaExe);
        commandLine.addArgument("-Dplatform.server.settingsPath=conf/scripted/settings.xml");
//        commandLine.addArgument("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");

        String rmiServerHostname = System.getProperty("java.rmi.server.hostname");
        if (rmiServerHostname != null) {
            commandLine.addArgument("-Djava.rmi.server.hostname=" + rmiServerHostname);
        }

        commandLine.addArgument("-cp");
        commandLine.addArgument(tempProjectDir.getAbsolutePath() + System.getProperty("path.separator") + System.getProperty("java.class.path"));
        commandLine.addArgument(BusinessLogicsBootstrap.class.getName());
        System.out.println(commandLine.toString());

        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(new NullOutputStream(), new NullOutputStream()));
//        executor.setStreamHandler(new PumpStreamHandler());
        executor.setExitValue(0);

        executor.execute(commandLine, new ManagedLogicsExecutionHandler(configurationId));
    }

    private String createTemporaryScriptFile(File tempModulesDir, String moduleSource) throws IOException {
        File moduleFile = File.createTempFile("module", ".lsf", tempModulesDir);

        PrintStream ps = new PrintStream(new FileOutputStream(moduleFile), false, "UTF-8");
        ps.print(moduleSource);
        ps.close();

        return moduleFile.getAbsolutePath();
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

    public void notificationReceived(NotificationData notificationData) {
        logger.debug("Notification received from managed app: " + notificationData);

        int configurationId = notificationData.configurationId;
        String eventType = notificationData.eventType;
        if (STARTED.equals(eventType)) {
            paas.changeConfigurationStatus(configurationId, "started");
        } else if (STOPPED.equals(eventType)) {
            paas.changeConfigurationStatus(configurationId, "stopped");
        } else if (ERROR.equals(eventType)) {
            logger.error("Error on managed app: " + notificationData.message);
            pushConfigurationLaunchError(configurationId, notificationData.message);
        }
    }

    private void pushConfigurationLaunchError(int configurationId, String errorMsg) {
        paas.pushConfigurationLaunchError(configurationId, errorMsg);
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

            pushConfigurationLaunchError(configurationId, "Error executing process: " + e.getMessage());
        }
    }
}
