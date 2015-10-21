package lsfusion.client;

import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.dock.DockableMainFrame;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.form.ClientExternalScreen;
import lsfusion.client.form.editor.rich.RichEditorPane;
import lsfusion.client.remote.proxy.RemoteFormProxy;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.client.rmi.RMITimeoutSocketFactory;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.GUIPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.event.EventBus;
import lsfusion.interop.event.IDaemonTask;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.text.DateFormat;
import java.util.*;

import static lsfusion.base.DateConverter.*;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.*;
import static lsfusion.interop.remote.RMIUtils.initRMI;

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class);

    public static final String LSFUSION_TITLE = "lsFusion";
    public static final String DEFAULT_SPLASH_PATH = "/images/lsfusion.jpg";

    public static final File fusionDir = new File(System.getProperty("user.home"), ".fusion");

    public static final RMITimeoutSocketFactory rmiSocketFactory = RMITimeoutSocketFactory.getInstance();

    public static boolean configurationAccessAllowed;

    public static ModuleFactory module;

    public static RemoteLogicsLoaderInterface remoteLoader;
    public static RemoteLogicsInterface remoteLogics;
    public static RemoteNavigatorInterface remoteNavigator;

    public static String logicsName;
    public static String logicsDisplayName;
    public static byte[] logicsMainIcon;
    public static byte[] logicsLogo;
    
    public static boolean hideMenu;

    public static int computerId;
    public static DateFormat dateFormat;
    public static DateFormat dateEditFormat;
    public static DateFormat timeFormat;
    public static DateFormat timeEditFormat;
    public static DateFormat dateTimeFormat;
    public static DateFormat dateTimeEditFormat;
    public static Date wideFormattableDate;
    public static Date wideFormattableDateTime;

    public static MainFrame frame;

    public static int asyncTimeOut;

    public static EventBus eventBus = new EventBus();
    private static ArrayList<IDaemonTask> daemonTasks;

    static SingleInstance singleInstance;

    public static void start(final String[] args, ModuleFactory startModule) {

        registerSingleInstanceListener();

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

            initRMI(rmiSocketFactory);

            initSwing();
        } catch (Exception e) {
            logger.error("Error during startup: ", e);
            e.printStackTrace();
            removeSingleInstanceListener();
            System.exit(1);
        }

        startWorkingThreads();
    }

    private static void registerSingleInstanceListener() {
        if(Boolean.parseBoolean(getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_SINGLEINSTANCE))) {

            Class singleInstanceServiceClass = classForName("javax.jnlp.SingleInstanceService");
            Class singleInstanceClass = singleInstanceServiceClass != null ? classForName("lsfusion.client.SingleInstanceImpl") : null;
            if (singleInstanceClass != null) {
                try {
                    singleInstance = (SingleInstance) singleInstanceClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    singleInstance = null;
                }
                if (singleInstance != null) {
                    singleInstance.register();
                }
            }
        }
    }

    private static Class classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static void removeSingleInstanceListener() {
        if(singleInstance != null)
            singleInstance.unregister();
    }

    public static String getSystemPropertyWithJNLPFallback(String propertyName) {
        String value = System.getProperty(propertyName);
        return value != null ? value : System.getProperty("jnlp." + propertyName);
    }

    private static void startWorkingThreads() {
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

                    String country = remoteLogics.getUserCountry();
                    String language = remoteLogics.getUserLanguage();
                    if (country != null && language != null) {
                        Locale.setDefault(new Locale(language, country));
                        ClientResourceBundle.clientResourceBundle = ResourceBundle.getBundle("ClientResourceBundle"); // чтобы подставлялась нужная локаль
                    }

                    GUIPreferences prefs = remoteLogics.getGUIPreferences();
                    logicsName = prefs.logicsName;
                    logicsDisplayName = prefs.logicsDisplayName;
                    logicsMainIcon = prefs.logicsMainIcon;
                    logicsLogo = prefs.logicsLogo;
                    hideMenu = prefs.hideMenu;

                    setupTimeZone();

                    remoteNavigator = loginAction.getRemoteNavigator();
                    computerId = loginAction.getComputerId();

                    configurationAccessAllowed = remoteNavigator.isConfigurationAccessAllowed();

                    startSplashScreen();

                    logger.info("Before init frame");
                    frame = module.initFrame(remoteNavigator);
                    logger.info("After init frame");

                    remoteNavigator.setUpdateTime(pullMessagesPeriod);

                    frame.addWindowListener(
                            new WindowAdapter() {
                                public void windowOpened(WindowEvent e) {
                                    closeSplashScreen();
                                }
                            }
                    );

                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    logger.info("After setExtendedState");

                    ConnectionLostManager.start(frame, remoteNavigator.getClientCallBack());

                    frame.setVisible(true);

                    ((DockableMainFrame) frame).focusPageIfNeeded();

                    daemonTasks = remoteLogics.getDaemonTasks(Main.computerId);
                    for (IDaemonTask task : daemonTasks) {
                        task.setEventBus(eventBus);
                        task.start();
                    }
                } catch (Exception e) {
                    closeSplashScreen();
                    logger.error(getString("client.error.application.initialization"), e);
                    Log.error(getString("client.error.application.initialization"), e, true);
                    Main.restart();
                }
            }
        });
    }

    private static void setupTimeZone() throws RemoteException {
        TimeZone timeZone = TimeZone.getTimeZone(remoteLogics.getUserTimeZone());
        if (timeZone != null) {
            TimeZone.setDefault(timeZone);
        }
        
        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

//        timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

//        dateTimeFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

        timeEditFormat = createTimeEditFormat(timeFormat);
        dateEditFormat = createDateEditFormat(dateFormat);
        dateTimeEditFormat = createDateTimeEditFormat(dateTimeFormat);

        wideFormattableDate = createWideFormattableDate();
        wideFormattableDateTime = createWideFormattableDate();
    }

    private static Date createWideFormattableDate() {
        GregorianCalendar gc2 = new GregorianCalendar();
        //просто любая дата, для которой нужны обе цифры при форматтинге
        gc2.set(1991, Calendar.NOVEMBER, 21, 10, 55, 55);
        return gc2.getTime();
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

    private static void initSwing() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
//        FocusOwnerTracer.installFocusTracer();

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        // при первом использовании rich-editora во время редактирования, его создание тормозит...
        // возможно, где-то внутри кэшируются какие-то lazy-ресурсы... Чтобы это не напрягало на форме, создаём компонент вхолостую здесь
        new RichEditorPane();
    }

    public static boolean clientExceptionLog(String title, Throwable t) throws RemoteException {
        if (remoteNavigator != null) {
            remoteNavigator.logClientException(title, SystemUtils.getLocalHostName(), t);
            return true;
        }
        return false;
    }

    public static void setStatusText(String msg) {
        if (frame != null) {
            frame.statusComponent.setText(msg);
        }
    }

    public static long getBytesSent() {
        return rmiSocketFactory.outSum;
    }

    public static long getBytesReceived() {
        return rmiSocketFactory.inSum;
    }

    public static void overrideRMIHostName(String hostName) {
        rmiSocketFactory.setOverrideHostName(hostName);
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
        return loadResource(logicsMainIcon, LSFUSION_CLIENT_LOGO, DEFAULT_SPLASH_PATH);
    }

    public static ImageIcon getLogo() {
        return loadResource(logicsLogo, LSFUSION_CLIENT_LOGO, DEFAULT_SPLASH_PATH);
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
        return BaseUtils.nvl(BaseUtils.nullEmpty(logicsDisplayName), LSFUSION_TITLE);
    }

    public static void shutdown() {
        SwingUtils.assertDispatchThread();

        ConnectionLostManager.invalidate();

        //даём немного времени на обработку текущих событий
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clean();
            }
        });
        timer.setRepeats(false);
        timer.start();

        // закрываемся в отдельном потоке, чтобы обработались текущие события (в частности WINDOW_CLOSING)
        final Thread closer = new Thread("Closing thread...") {
            @Override
            public void run() {
                //убиваемся, если через 5 секунд ещё не вышли
                removeSingleInstanceListener();
                SystemUtils.sleep(5000);
                System.exit(0);
            }
        };
        closer.setDaemon(true);
        closer.start();
    }

    public static void restart() {
        restart(false);
    }

    public static void reconnect() {
        restart(true);
    }

    private static void restart(final boolean reconnect) {
        SwingUtils.assertDispatchThread();

        ConnectionLostManager.invalidate();

        LoginAction.getInstance().setAutoLogin(reconnect);

        //даём немного времени на обработку текущих событий
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clean();
                startWorkingThreads();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void clean() {

        try {
            remoteNavigator.close();
        } catch (Throwable ignore) {
        }

        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }

        RemoteFormProxy.dropCaches();
        ClientExternalScreen.dropCaches();

        if (daemonTasks != null) {
            for (IDaemonTask task : daemonTasks) {
                task.stop();
            }
        }
        eventBus.invalidate();

        computerId = -1;
        frame = null;
        remoteLoader = null;
        remoteLogics = null;
        remoteNavigator = null;

        System.gc();
    }

    public static int generateID() throws RemoteException {
        return remoteLogics.generateID();
    }

    public static void closeHangingSockets() {
        rmiSocketFactory.closeHangingSockets();
    }

    public interface ModuleFactory {
        MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws IOException;

        void openInExcel(ReportGenerationData generationData);

        boolean isFull();
    }

    public static void main(final String[] args) {

//        SpanningTreeWithBlackjack.test();
//        SpanningTreeWithBlackjack.test1();
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws IOException {
                return new DockableMainFrame(remoteNavigator);
            }

            public void openInExcel(ReportGenerationData generationData) {
                ReportGenerator.exportToExcelAndOpen(generationData, FormPrintType.XLSX);
            }

            public boolean isFull() {
                return true;
            }
        });
    }
}