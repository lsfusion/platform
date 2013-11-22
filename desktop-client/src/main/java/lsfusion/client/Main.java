package lsfusion.client;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.base.ApiResourceBundle;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.dock.DockableMainFrame;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.exceptions.ExceptionThreadGroup;
import lsfusion.client.form.ClientExternalScreen;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.classes.ClientTypeSerializer;
import lsfusion.client.remote.proxy.RemoteFormProxy;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.client.rmi.RMITimeoutSocketFactory;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.event.EventBus;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.*;

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class);

    public static final String LSFUSION_TITLE = "lsFusion";
    private static final String DEFAULT_SPLASH_PATH = "/images/lsfusion.jpg";

    public static boolean configurationAccessAllowed;

    public static ModuleFactory module;

    public static RemoteLogicsLoaderInterface remoteLoader;
    public static RemoteLogicsInterface remoteLogics;
    public static RemoteNavigatorInterface remoteNavigator;

    public static int computerId;
    public static TimeZone timeZone;
    public static DateFormat dateFormat;
    public static DateFormat timeFormat;
    public static DateFormat dateTimeFormat;

    public static MainFrame frame;

    public static int asyncTimeOut;

    private static ThreadGroup bootstrapThreadGroup;
    private static ExceptionThreadGroup mainThreadGroup;
    private static Thread mainThread;
    private static PingThread pingThread;

    private static RMITimeoutSocketFactory socketFactory;

    private static ClientObjectClass baseClass = null;
    public static EventBus eventBus = new EventBus();
    private static ScheduledExecutorService daemonTasksExecutor;

    public static void start(final String[] args, ModuleFactory startModule) {
        bootstrapThreadGroup = Thread.currentThread().getThreadGroup();

        module = startModule;

        System.setProperty("sun.awt.exception.handler", ClientExceptionManager.class.getName());

//        Это нужно, чтобы пофиксать баг, когда форма не собирается GC...
//        http://stackoverflow.com/questions/2808582/memory-leak-with-swing-drag-and-drop/2860372#2860372
        System.setProperty("swing.bufferPerWindow", "false");

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов
        System.setProperty("sun.rmi.dgc.client.gcInterval", "60000");

        asyncTimeOut = Integer.parseInt(System.getProperty(LSFUSION_CLIENT_ASYNC_TIMEOUT, "50"));

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                ClientExceptionManager.handle(e);
            }
        });

        try {
            initJulLogging();

            loadLibraries();

            initRmiClassLoader();

            initRMISocketFactory();

            initSwing();
        } catch (Exception e) {
            logger.error("Error during startup: ", e);
            e.printStackTrace();
            System.exit(1);
        }

        startWorkingThreads();
    }

    private static void startWorkingThreads() {
        startWorkingThreads(false);
    }

    private static void startWorkingThreads(boolean reconnect) {
        mainThreadGroup = new ExceptionThreadGroup();
        mainThread = new Thread(mainThreadGroup, "Init thread") {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                            LoginAction loginAction = LoginAction.getInstance();
                            if (!loginAction.login()) {
                                return;
                            }

                            remoteLogics = loginAction.getRemoteLogics();
                            remoteNavigator = loginAction.getRemoteNavigator();
                            computerId = loginAction.getComputerId();

                            setupTimeZone();

                            ClientExceptionManager.flushUnreportedThrowables();

                            configurationAccessAllowed = remoteNavigator.isConfigurationAccessAllowed();

                            startSplashScreen();

                            logger.info("Before init frame");
                            frame = module.initFrame(remoteNavigator);
                            logger.info("After init frame");

                            pingThread = new PingThread(remoteNavigator.getClientCallBack());
                            pingThread.start();
                            remoteNavigator.setUpdateTime(pingThread.updateTime);

                            frame.addWindowListener(
                                    new WindowAdapter() {
                                        public void windowOpened(WindowEvent e) {
                                            closeSplashScreen();
                                        }

                                        public void windowClosing(WindowEvent e) {
                                            try {
                                                remoteLogics.endSession(SystemUtils.getLocalHostName() + " " + computerId);
                                            } catch (Exception ex) {
                                                throw new RuntimeException(ex);
                                            }
                                        }
                                    }
                            );

                            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                            logger.info("After setExtendedState");

                            ConnectionLostManager.install(frame);

                            frame.setVisible(true);

                            ((DockableMainFrame) frame).focusPageIfNeeded();

                            ArrayList<IDaemonTask> tasks = remoteLogics.getDaemonTasks(Main.computerId);
                            daemonTasksExecutor = Executors.newScheduledThreadPool(1);
                            for (IDaemonTask task : tasks) {
                                task.setEventBus(eventBus);
                                daemonTasksExecutor.scheduleWithFixedDelay(new DaemonTask(task), task.getDelay(), task.getPeriod(), TimeUnit.MILLISECONDS);
                            }
                        } catch (Exception e) {
                            closeSplashScreen();
                            logger.error(getString("client.error.application.initialization"), e);
                            throw new RuntimeException(getString("client.error.application.initialization"), e);
                        }
                    }
                });
            }
        };
        mainThread.start();
    }

    private static void setupTimeZone() throws RemoteException {
        timeZone = remoteLogics.getTimeZone();

        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        dateFormat.setTimeZone(timeZone);

//        timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

//        dateTimeFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    }

    private static void initJulLogging() {
//        ClientLoggingManager.turnOnTcpLogging();
        boolean turnOnRmiLogging = Boolean.getBoolean(LSFUSION_CLIENT_LOG_RMI);
        if (turnOnRmiLogging) {
            String logBaseDir = System.getProperty(LSFUSION_CLIENT_LOG_BASEDIR);
            if (logBaseDir != null) {
                ClientLoggingManager.turnOnRmiLogging(logBaseDir);
            } else {
                ClientLoggingManager.turnOnRmiLogging();
            }
        }
    }

    // будет загружать все не кросс-платформенные библиотеки
    private static void loadLibraries() throws IOException {
        ComBridge.loadJacobLibraries();
        ComBridge.loadJsscLibraries();
    }

    private static void initRmiClassLoader() throws IllegalAccessException, NoSuchFieldException {
        // приходится извращаться, так как RMIClassLoader использует для загрузки Spi Class.forname,
        // а это работает некорректно, поскольку JWS использует свой user-class loader,
        // а сами jar-файлы не добавляются в java.class.path
        // необходимо, чтобы ClientRMIClassLoaderSpi запускался с родным ClassLoader JWS

        Field field = RMIClassLoader.class.getDeclaredField("provider");
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, new ClientRMIClassLoaderSpi());

        // сбрасываем SecurityManager, который устанавливает JavaWS,
        // поскольку он не дает ничего делать классу ClientRMIClassLoaderSpi,
        // так как он load'ится из временного директория
        System.setSecurityManager(null);
    }

    private static void initRMISocketFactory() throws IOException {
        String timeout = System.getProperty(LSFUSION_CLIENT_CONNECTION_LOST_TIMEOUT, "7200000");

        if (RMISocketFactory.getSocketFactory() != null) {
            System.out.println(RMISocketFactory.getSocketFactory());
        }

        socketFactory = new RMITimeoutSocketFactory(Integer.valueOf(timeout));

        RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
            public boolean failure(Exception ex) {
                logger.error(ApiResourceBundle.getString("exceptions.rmi.error") + " ", ex);
                return true;
            }
        });

        try {
            Field field = RMISocketFactory.class.getDeclaredField("factory");
            field.setAccessible(true);
            field.set(null, socketFactory);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initSwing() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
//        FocusOwnerTracer.installFocusTracer();

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    }

    public static ClientObjectClass getBaseClass() {
        if (baseClass == null) {
            try {
                baseClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(
                        new DataInputStream(new ByteArrayInputStream(
                                remoteLogics.getBaseClassByteArray())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return baseClass;
    }

    public static void clientExceptionLog(String info, String client, String message, String type, String erTrace) throws RemoteException {
        if (remoteNavigator != null) {
            remoteNavigator.logClientException(info, client, message, type, erTrace);
        }
    }

    public static void setStatusText(String msg) {
        if (frame != null) {
            frame.statusComponent.setText(msg);
        }
    }

    public static long getBytesSent() {
        return socketFactory.outSum;
    }

    public static long getBytesReceived() {
        return socketFactory.inSum;
    }

    public static void overrideRMIHostName(String hostName) {
        socketFactory.setOverrideHostName(hostName);
    }

    public static void closeHangingSockets() {
        socketFactory.closeHangingSockets();
    }

    private static void startSplashScreen() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SplashScreen.start(getLogo());
            }
        });
    }

    private static void closeSplashScreen() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SplashScreen.close();
            }
        });
    }

    public static int generateNewID() {
        try {
            return remoteLogics.generateNewID();
        } catch (RemoteException e) {
            throw new RuntimeException(getString("client.error.on.id.generation"));
        }
    }

    public static ImageIcon getMainIcon() {
        byte[] iconData = null;
        if (remoteLogics != null) {
            try {
                iconData = remoteLogics.getMainIcon();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return loadResource(iconData, LSFUSION_CLIENT_LOGO, DEFAULT_SPLASH_PATH);
    }

    public static ImageIcon getLogo() {
        byte[] logoData = null;
        if (remoteLogics != null) {
            try {
                logoData = remoteLogics.getLogo();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return loadResource(logoData, LSFUSION_CLIENT_LOGO, DEFAULT_SPLASH_PATH);
    }

    private static ImageIcon loadResource(byte[] resourceData, String defaultUrlSystemPropName, String defaultResourcePath) {
        ImageIcon resource = resourceData != null ? new ImageIcon(resourceData) : null;
        if (resource == null || resource.getImageLoadStatus() != MediaTracker.COMPLETE) {
            String splashUrlString = System.getProperty(defaultUrlSystemPropName);
            URL splashUrl = null;
            if (splashUrlString != null) {
                try {
                    splashUrl = new URL(splashUrlString);
                } catch (MalformedURLException ignored) {
                }
            }
            if (splashUrl != null) {
                resource = new ImageIcon(splashUrl);
            }

            if (resource == null || resource.getImageLoadStatus() != MediaTracker.COMPLETE) {
                resource = new ImageIcon(SplashScreen.class.getResource(defaultResourcePath));
            }
        }
        return resource;
    }

    public static String getMainTitle() {
        return BaseUtils.nvl(getDisplayName(), LSFUSION_TITLE);
    }

    public static String getDisplayName() {
        String title = null;
        if (remoteLogics != null) {
            try {
                title = remoteLogics.getDisplayName();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return title;
    }

    public static void shutdown() {
        final Thread closer = new Thread(bootstrapThreadGroup, new Runnable() {
            @Override
            public void run() {
                clean();

                // закрываемся в EDT, чтобы обработались текущие события (в частности WINDOW_CLOSING)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                });
            }
        }, "Restarting thread...");
        closer.setDaemon(false);
        closer.start();
    }

    public static void restart() {
        restart(false);
    }

    public static void reconnect() {
        restart(true);
    }

    public static void restart(final boolean reconnect) {
        LoginAction.getInstance().setAutoLogin(reconnect);
        final Thread restarter = new Thread(bootstrapThreadGroup, new Runnable() {
            @Override
            public void run() {
                clean();

                startWorkingThreads(reconnect);
            }
        }, "Restarting thread...");
        restarter.setDaemon(false);
        restarter.start();
    }

    private static void clean() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ConnectionLostManager.invalidate();

                    SplashScreen.close();

                    if (frame != null) {
                        frame.setVisible(false);
                        frame.dispose();
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        mainThreadGroup.interrupt();

        RemoteFormProxy.dropCaches();
        ClientExternalScreen.dropCaches();

        eventBus.invalidate();
        daemonTasksExecutor.shutdown();
        try {
            daemonTasksExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }

        computerId = -1;
        timeZone = null;
        baseClass = null;
        frame = null;
        remoteLoader = null;
        remoteLogics = null;
        remoteNavigator = null;

        System.gc();
    }

    static class DaemonTask extends TimerTask {
        IDaemonTask task;

        public DaemonTask(IDaemonTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            task.run();
        }
    }

    public interface ModuleFactory {
        MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws IOException;

        void openInExcel(ReportGenerationData generationData);

        boolean isFull();
    }

    public static void main(final String[] args) {
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws IOException {
                return new DockableMainFrame(remoteNavigator);
            }

            public void openInExcel(ReportGenerationData generationData) {
                ReportGenerator.exportToExcelAndOpen(generationData, timeZone);
            }

            public boolean isFull() {
                return true;
            }
        });
    }
}