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
import paas.terminal.ApplicationTerminal;
import platform.base.IOUtils;
import platform.base.NullOutputStream;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.ServerLoggers;
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
import platform.server.logics.RMIManager;
import platform.server.session.DataSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static platform.base.BaseUtils.isRedundantString;
import static platform.base.BaseUtils.nvl;
import static platform.base.SystemUtils.isPortAvailable;
import static platform.server.lifecycle.LifecycleEvent.*;

public final class AppManager extends LifecycleAdapter implements InitializingBean {
    private final static Logger logger = ServerLoggers.systemLogger;

    private static final String javaExe = System.getProperty("java.home") + "/bin/java";

    private final ChannelGroup openChannels = new DefaultChannelGroup();

    private final AppManagerProcessDestroyer appManagerProcessDestroyer = new AppManagerProcessDestroyer(this);

    private ChannelFactory channelFactory;

    private boolean started = false;

    private RMIManager rmiManager;

    private int acceptPort;

    private int registryPort;

    private Context instanceContext;

    private PaasBusinessLogics paas;

    private PaasLogicsModule paasLM;

    private String dbServer;
    private String dbUser;
    private String dbPassword;

    //todo: locksMap[configId -> lock] for performance
    private final Object configurationStatusLock = new Object();

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        instanceContext = logicsInstance.getContext();
    }

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setBusinessLogics(PaasBusinessLogics businessLogics) {
        this.paas = businessLogics;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
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
        Assert.notNull(rmiManager, "rmiManager must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(instanceContext, "logicsInstance must be specified");

        Assert.state(0 <= registryPort && registryPort <= 65535, "registryPort must be between 0 and 65535");
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
                Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(instanceContext, "-app-manager-daemon-")));

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

        //сначала завершаем все managed-логики, чтобы они ещё могли сообщить об этом
        appManagerProcessDestroyer.shutdown();

        ChannelGroupFuture future = openChannels.close();
        future.awaitUninterruptibly();
        channelFactory.releaseExternalResources();
        started = false;
    }

    public void addOpenedChannel(Channel channel) {
        openChannels.add(channel);
    }

    public String getStatus(String exportName) {
        try {
            getManagedAppTerminal(exportName);
            return "started";
        } catch (Exception e) {
            return "stopped";
        }
    }

    public void stopApplication(String exportName) throws RemoteException, MalformedURLException, NotBoundException {
        getManagedAppTerminal(exportName).stop();
    }

    private ApplicationTerminal getManagedAppTerminal(String exportName) throws NotBoundException, MalformedURLException, RemoteException {
        exportName = exportName.trim();
        return (ApplicationTerminal) Naming.lookup(format("rmi://localhost:%d/%s/AppTerminal", registryPort, exportName));
    }

    public void executeConfiguration(DataSession session, DataObject confObj) throws IOException, InterruptedException, SQLException {

        Integer exportPort = (Integer) nvl(paasLM.configurationPort.read(session, confObj), 0);

        PaasUtils.checkPortExceptionally(exportPort);

        if (exportPort != 0 && !isPortAvailable(exportPort)) {
            throw new IllegalStateException("Port is busy.");
        }

        String dbName = (String) paasLM.configurationDatabaseName.read(session, confObj);
        if (dbName == null) {
            throw new IllegalStateException("DB name is empty.");
        }

        dbName = dbName.trim();

        String exportName = (String) paasLM.configurationExportName.read(session, confObj);
        if (isRedundantString(exportName)) {
            throw new IllegalStateException("exportName is empty.");
        }

        Integer projId = (Integer) paasLM.configurationProject.read(session, confObj);

        ImRevMap<String, KeyExpr> keys = KeyExpr.getMapKeys(SetFact.singleton("moduleKey"));
        Expr moduleExpr = keys.get("moduleKey");
        Expr projExpr = new DataObject(projId, paasLM.project).getExpr();

        QueryBuilder<String, String> q = new QueryBuilder<String, String>(keys);
        q.and(
                paasLM.moduleInProject.getExpr(session.getModifier(), projExpr, moduleExpr).getWhere()
        );
        q.addProperty("moduleSource", paasLM.moduleSource.getExpr(session.getModifier(), moduleExpr));

        ImOrderMap<ImMap<String,Object>, ImMap<String, Object>> values = q.execute(session.sql);

        //подготавливаем файлы для запуска
        File tempProjectDir = IOUtils.createTempDirectory("paas-project");
        File tempModulesDir = new File(tempProjectDir, "paasmodules");
        tempModulesDir.mkdir();

        List<String> moduleFilePaths = new ArrayList<String>();
        for (ImMap<String, Object> entry : values.valueIt()) {
            String moduleSource = nvl((String) entry.get("moduleSource"), "");
            moduleFilePaths.add(createTemporaryScriptFile(tempModulesDir, moduleSource));
        }

        int configurationId = (Integer) confObj.object;

        boolean canStart = false;
        synchronized (configurationStatusLock) {
            if ("stopped".equals(paas.getConfigurationStatus(configurationId))) {
                canStart = true;
                paas.changeConfigurationStatus(confObj, "init");
            }
        }

        if (canStart) {
            executeScriptedBL(configurationId, dbName, exportName, exportPort,  tempProjectDir, moduleFilePaths);
        }
    }

    private void executeScriptedBL(int configurationId, String dbName, String exportName, int exportPort, File tempProjectDir, List<String> scriptFilePaths) throws IOException, InterruptedException {
        exportName = exportName.trim();

        Properties properties = new Properties();
        properties.setProperty("db.server", dbServer);
        properties.setProperty("db.name", dbName);
        properties.setProperty("db.user", dbUser);
        properties.setProperty("db.password", dbPassword);
        properties.setProperty("rmi.registryPort", String.valueOf(registryPort));
        properties.setProperty("rmi.exportPort", String.valueOf(exportPort));
        properties.setProperty("rmi.exportName", exportName);
        properties.setProperty("logics.overridingModulesList", "");
        properties.setProperty("logics.includedPaths", "paasmodules/");
        properties.setProperty("logics.excludedPaths", "");
        properties.setProperty("paas.manager.host", "localhost");
        properties.setProperty("paas.manager.port", String.valueOf(acceptPort));
        properties.setProperty("paas.configurationId", String.valueOf(configurationId));
        FileOutputStream out = new FileOutputStream(new File(tempProjectDir, "settings.properties"));
        properties.store(out, null);
        out.close();

        CommandLine commandLine = new CommandLine(javaExe);
//        commandLine.addArgument("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
        commandLine.addArgument("-Dplatform.server.settingsPath=conf/scripted/settings.xml");
        commandLine.addArgument("-cp");
        commandLine.addArgument(tempProjectDir.getAbsolutePath() + System.getProperty("path.separator") + System.getProperty("java.class.path"));
        commandLine.addArgument(BusinessLogicsBootstrap.class.getName());

        Executor executor = new DefaultExecutor();
        executor.setProcessDestroyer(appManagerProcessDestroyer);
        executor.setStreamHandler(new PumpStreamHandler(new NullOutputStream(), new NullOutputStream()));
        executor.setExitValue(0);

        executor.execute(commandLine, new ManagedLogicsExecutionHandler(configurationId, exportName));
    }

    private String createTemporaryScriptFile(File tempModulesDir, String moduleSource) throws IOException {
        File moduleFile = File.createTempFile("module", ".lsf", tempModulesDir);

        PrintStream ps = new PrintStream(new FileOutputStream(moduleFile), false, "UTF-8");
        ps.print(moduleSource);
        ps.close();

        return moduleFile.getAbsolutePath();
    }

    public void notificationReceived(NotificationData notificationData) {
        logger.debug("Notification received from managed app: " + notificationData);

        int configurationId = notificationData.configurationId;
        String eventType = notificationData.eventType;
        if (INIT.equals(eventType)) {
            paas.changeConfigurationStatus(configurationId, "init");
        } else if (STARTED.equals(eventType)) {
            paas.changeConfigurationStatus(configurationId, "started");
        } else if (STOPPING.equals(eventType)) {
            paas.changeConfigurationStatus(configurationId, "stopping");
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

    private void cleanAfterManagedAppFailed(int configurationId, String exportName) {
        paas.changeConfigurationStatus(configurationId, "stopped");

        String bindings[];
        try {
            bindings = rmiManager.list();
        } catch (RemoteException e) {
            logger.error("Can't list registry bindings.");
            return;
        }

        exportName += "/";

        for (String binding : bindings) {
            if (binding.startsWith(exportName)) {
                try {
                    rmiManager.unbind(binding);
                } catch (RemoteException e) {
                    logger.error("Error unbinding '" + binding + "': ", e);
                } catch (NotBoundException ignore) {
                    //возможно, что бинда уже может не быть
                }
            }
        }
    }

    private class ManagedLogicsExecutionHandler extends DefaultExecuteResultHandler {
        private final int configurationId;
        private final String exportName;

        public ManagedLogicsExecutionHandler(int configurationId, String exportName) {
            this.configurationId = configurationId;
            this.exportName = exportName;

            appManagerProcessDestroyer.addExportedName(exportName);
        }

        @Override
        public void onProcessComplete(int exitValue) {
            super.onProcessComplete(exitValue);

            appManagerProcessDestroyer.removeExportedName(exportName);
        }

        @Override
        public void onProcessFailed(ExecuteException e) {
            super.onProcessFailed(e);

            logger.error("Error executing process: " + e.getMessage(), e.getCause());

            appManagerProcessDestroyer.removeExportedName(exportName);

            cleanAfterManagedAppFailed(configurationId, exportName);

            pushConfigurationLaunchError(configurationId, "Error executing process: " + e.getMessage());
        }
    }
}
