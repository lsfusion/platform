package lsfusion.client;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.FileData;
import lsfusion.client.authentication.LoginAction;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.log.ClientLoggingManager;
import lsfusion.client.base.dock.DockableMainFrame;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.base.Log;
import lsfusion.client.base.equ.ComBridge;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.print.SavingThread;
import lsfusion.client.form.property.classes.editor.rich.RichEditorPane;
import lsfusion.client.remote.ClientRMIClassLoaderSpi;
import lsfusion.client.form.remote.proxy.RemoteFormProxy;
import lsfusion.client.remote.ConnectionLostManager;
import lsfusion.client.remote.RMITimeoutSocketFactory;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.event.EventBus;
import lsfusion.interop.form.event.ICleanListener;
import lsfusion.interop.logics.RemoteLogicsInterface;
import lsfusion.interop.logics.RemoteLogicsLoaderInterface;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import sun.awt.OSInfo;
import sun.awt.SunToolkit;
import sun.security.action.GetPropertyAction;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.security.AccessController;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.base.DateConverter.*;
import static lsfusion.base.remote.RMIUtils.initRMI;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.*;

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class);

    public static final String LSFUSION_TITLE = "lsFusion";
    public static final String DEFAULT_ICON_PATH = "/images/logo/";

    public static final File fusionDir = new File(System.getProperty("user.home"), ".fusion");

    public static final RMITimeoutSocketFactory rmiSocketFactory = RMITimeoutSocketFactory.getInstance();

    public static RemoteLogicsLoaderInterface remoteLoader;
    public static RemoteLogicsInterface remoteLogics;
    public static RemoteNavigatorInterface remoteNavigator;

    public static String logicsName;
    public static String logicsDisplayName;
    public static byte[] logicsMainIcon;
    public static byte[] logicsLogo;

    public static String computerName;
    public static DateFormat dateFormat;
    public static DateFormat dateEditFormat;
    public static DateFormat timeFormat;
    public static DateFormat timeEditFormat;
    public static DateFormat dateTimeFormat;
    public static DateFormat dateTimeEditFormat;
    public static Date wideFormattableDate;
    public static Date wideFormattableDateTime;

    public static MainFrame frame;

    public static ClientFormController currentForm;

    public static int asyncTimeOut;

    public static EventBus eventBus = new EventBus();
    public static List<ICleanListener> cleanListeners = new ArrayList<>();

    static SingleInstance singleInstance;
    public static boolean busyDialog;
    public static long busyDialogTimeout;
    public static boolean useRequestTimeout;
    public static boolean configurationAccessAllowed;
    public static boolean forbidDuplicateForms;

    public static long timeDiffServerClientLog = 1000;

    public static Integer fontSize;
    private static Map<Object, FontUIResource> fontUIDefaults = new HashMap<>();

    public static void start(final String[] args) {

        registerSingleInstanceListener();

        computerName = SystemUtils.getLocalHostName();

        System.setProperty("sun.awt.exception.handler", ClientExceptionManager.class.getName());

//        Это нужно, чтобы пофиксать баг, когда форма не собирается GC...
//        http://stackoverflow.com/questions/2808582/memory-leak-with-swing-drag-and-drop/2860372#2860372
        System.setProperty("swing.bufferPerWindow", "false");

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов
//        System.setProperty("sun.rmi.dgc.client.gcInterval", "60000");

        // попытка исправить падающий иногда IllegalArgumentException, связанный с TimSort. исправлено в Java9
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7075600
        // правда, есть вероятность, что System.setProperty() не поможет, а нужно проставлять свойство JVM в начале: "-Djava.util.Arrays.useLegacyMergeSort=true"
        // см http://stackoverflow.com/a/26829874
        Double javaVersion = SystemUtils.getJavaSpecificationVersion();
        if (javaVersion == null || javaVersion < 1.9) {
            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");    
        }

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
                    LoginAction loginAction = LoginAction.getInstance();

                    loginAction.initLoginDialog();

                    if (!loginAction.login()) {
                        return;
                    }

                    remoteLogics = loginAction.getRemoteLogics();

                    remoteNavigator = loginAction.getRemoteNavigator();

                    ClientSettings clientSettings = remoteNavigator.getClientSettings();

                    LocalePreferences userPreferences = clientSettings.localePreferences;
                    fontSize = clientSettings.fontSize;
                    busyDialog = clientSettings.busyDialog;
                    busyDialogTimeout = Math.max(clientSettings.busyDialogTimeout, 1000); //минимальный таймаут 1000мс
                    useRequestTimeout = clientSettings.useRequestTimeout;
                    configurationAccessAllowed = clientSettings.configurationAccessAllowed;
                    forbidDuplicateForms = clientSettings.forbidDuplicateForms;

                    Locale userLocale = userPreferences.locale;
                    if (userLocale != null) {
                        Locale.setDefault(userLocale);
                        
                        UIManager.getDefaults().setDefaultLocale(userLocale);
                        UIManager.getLookAndFeelDefaults().setDefaultLocale(userLocale);

                        JFileChooser.setDefaultLocale(userLocale);
                        JColorChooser.setDefaultLocale(userLocale);
                    }

                    setupTimePreferences(userPreferences.timeZone, userPreferences.twoDigitYearStart);

                    setUIFontSize();

                    startSplashScreen();

                    logger.info("Before init frame");
                    frame = new DockableMainFrame(remoteNavigator, clientSettings.currentUserName);
                    logger.info("After init frame");

                    frame.addWindowListener(
                            new WindowAdapter() {
                                public void windowOpened(WindowEvent e) {
                                    closeSplashScreen();
                                }
                            }
                    );

                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    logger.info("After setExtendedState");

                    ConnectionLostManager.start(frame, remoteNavigator.getClientCallBack(), clientSettings.devMode);

                    frame.setVisible(true);

                    ((DockableMainFrame) frame).clearForms();

                    ((DockableMainFrame) frame).executeNavigatorAction("SystemEvents.onDesktopClientStarted[]", 0, null, null);

                } catch (Exception e) {
                    closeSplashScreen();
                    logger.error(getString("client.error.application.initialization"), e);
                    Main.restart();
                }
            }
        });
    }

    public static JSONObject getServerSettings(RemoteLogicsInterface remoteLogics) throws RemoteException {
        ExternalResponse result = remoteLogics.exec(AuthenticationToken.ANONYMOUS, LoginAction.getSessionInfo(), "Service.getServerSettings[]", new ExternalRequest());
        return new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
    }

    private static void setupTimePreferences(String userTimeZone, Integer twoDigitYearStart) throws RemoteException {

        TimeZone timeZone = userTimeZone == null ? null : TimeZone.getTimeZone(userTimeZone);
        if (timeZone != null) {
            TimeZone.setDefault(timeZone);
        }

        Date twoDigitYearStartDate = null;
        if (twoDigitYearStart != null) {
            GregorianCalendar c = new GregorianCalendar(twoDigitYearStart, 0, 1);
            twoDigitYearStartDate = c.getTime();
        }
        
        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        if (twoDigitYearStartDate != null) {
            ((SimpleDateFormat) dateFormat).set2DigitYearStart(twoDigitYearStartDate);
        }

//        timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

//        dateTimeFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        if (twoDigitYearStartDate != null) {
            ((SimpleDateFormat) dateTimeFormat).set2DigitYearStart(twoDigitYearStartDate);
        }

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

    private static void setUIFontSize() {
        Enumeration keys = UIManager.getDefaults().keys();
        if (fontSize != null || !fontUIDefaults.isEmpty()) { // skip if fontSize was never set
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                FontUIResource defaultUIFont = fontUIDefaults.get(key);
                if (defaultUIFont == null) {
                    Object value = UIManager.get(key);
                    if (value instanceof FontUIResource) {
                        defaultUIFont = (FontUIResource) value;
                        fontUIDefaults.put(key, defaultUIFont);
                    }
                }
                if (defaultUIFont != null) {
                    UIManager.put(key, new FontUIResource(defaultUIFont.deriveFont(getUIFontSize(defaultUIFont.getSize()))));
                }
            }
        }
    }

    public static float getUIFontSize(int defaultSize) {
        return fontSize != null ? (float) defaultSize * fontSize / 100 : defaultSize;
    }

    public static int getIntUIFontSize(int defaultSize) {
        return (int) (getUIFontSize(defaultSize) + 0.5); // as deriveFont() does
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

        ToolTipManager.sharedInstance().setInitialDelay(1500);
        // через сколько после скрытия тултипа снова ждать Initial Delay до показа нового (не в рамках одного компонента)
        ToolTipManager.sharedInstance().setReshowDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        UIManager.setLookAndFeel(getSystemLookAndFeelClassName());
        
        // при первом использовании rich-editora во время редактирования, его создание тормозит...
        // возможно, где-то внутри кэшируются какие-то lazy-ресурсы... Чтобы это не напрягало на форме, создаём компонент вхолостую здесь
        new RichEditorPane();
    }

    public static String getSystemLookAndFeelClassName() {
        String systemLAF = AccessController.doPrivileged(new GetPropertyAction("swing.systemlaf"));
        if (systemLAF != null) {
            return systemLAF;
        }
        if (AccessController.doPrivileged(OSInfo.getOSTypeAction()) != OSInfo.OSType.WINDOWS) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            // for non-gnome linux environments
            if (toolkit instanceof SunToolkit && ((SunToolkit) toolkit).isNativeGTKAvailable()) {
                return "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
            }
        }

        return UIManager.getSystemLookAndFeelClassName();
    }

    public static boolean clientExceptionLog(Throwable t) throws RemoteException {
        if (remoteNavigator != null) {
            remoteNavigator.logClientException("", Main.computerName, t);
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

    public static List<Image> getMainIcons() {
        Set<Image> images = new LinkedHashSet<>();
        if(logicsMainIcon != null) {
            ImageIcon resource = new ImageIcon(logicsMainIcon);
            if(resource.getImageLoadStatus() == MediaTracker.COMPLETE) {
                images.add(resource.getImage());
            }
        } else {
            images.add(getImage(DEFAULT_ICON_PATH + "icon_256.png"));
            images.add(getImage(DEFAULT_ICON_PATH + "icon_64.png"));
            images.add(getImage(DEFAULT_ICON_PATH + "icon_48.png"));
            images.add(getImage(DEFAULT_ICON_PATH + "icon_32.png"));
            images.add(getImage(DEFAULT_ICON_PATH + "icon_16.png"));
        }
        return new ArrayList<>(images);
    }

    public static ImageIcon getLogo() {
        return logicsLogo != null ? new ImageIcon(logicsLogo) : getImageIcon(DEFAULT_ICON_PATH + "logo.png");
    }

    private static Image getImage(String resourcePath) {
        return getImageIcon(resourcePath).getImage();
    }

    private static ImageIcon getImageIcon(String resourcePath) {
        return new ImageIcon(SplashScreen.class.getResource(resourcePath));
    }

    public static String getMainTitle() {
        return nvl(BaseUtils.nullEmpty(logicsDisplayName), LSFUSION_TITLE);
    }

    public static void hide() {
        frame.setState(Frame.ICONIFIED);
    }

    public static void shutdown() {
        SwingUtils.assertDispatchThread();

        ConnectionLostManager.invalidate();

        //даём немного времени на обработку текущих событий
        Log.log("Shutdown");
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
                Log.log("Close thread");
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
        Log.log("Restart : " + reconnect);
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clean();
                //Перегружаем classLoader. Возможно, следует выполнять и другие действия из Main.start()
                try {
                    initRmiClassLoader();
                } catch (Exception ex) {
                    logger.error("Error during startup: ", ex);
                    ex.printStackTrace();
                    removeSingleInstanceListener();
                    System.exit(1);
                }
                startWorkingThreads();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void executeNotificationAction(final Integer idNotification) {
        try {
            if (currentForm != null)
                currentForm.executeNotificationAction(idNotification);
            else
                ((DockableMainFrame) frame).executeNavigatorAction(String.valueOf(idNotification), 2, new Runnable() {
                    @Override
                    public void run() {
                        executeNotificationAction(idNotification);
                    }
                }, null);
        } catch (IOException e) {
            logger.error("Error executing notification action: ", e);
        }
    }

    public static void setCurrentForm(ClientFormController currentForm) {
        Main.currentForm = currentForm;
    }

    public static void dropCurrentForm(ClientFormController form) {
        if(currentForm != null && currentForm.equals(form))
            currentForm = null;
    }

    public static void addReportPathList(List<ReportPath> reportPathList, String formSID) throws IOException {
        reportPathList.addAll(Main.remoteLogics.saveAndGetCustomReportPathList(formSID, false));
        editReportPathList(reportPathList);
    }
    public static void recreateReportPathList(List<ReportPath> reportPathList, String formSID) throws IOException {
        Main.remoteLogics.saveAndGetCustomReportPathList(formSID, true);
        editReportPathList(reportPathList);
    }
    public static void editReportPathList(List<ReportPath> reportPathList) throws IOException {
        for (ReportPath reportPath : reportPathList) {
            Desktop.getDesktop().open(new File(reportPath.customPath));
        }
        // не очень хорошо оставлять живой поток, но это используется только в девелопменте, поэтому не важно
        new SavingThread(reportPathList).start();
    }

    public static void deleteReportPathList(List<ReportPath> reportPathList) {
        for (ReportPath reportPath : reportPathList) {
            File customFile = new File(reportPath.customPath);
            if(!customFile.delete())
                customFile.deleteOnExit();
            File targetFile = new File(reportPath.targetPath);
            if(!targetFile.delete())
                targetFile.deleteOnExit();
        }
        reportPathList.clear();
    }

    private static void clean() {

        Log.log("Clean");
        try {
            if(remoteNavigator != null)
                remoteNavigator.close();
        } catch (Throwable ignore) {
        }

        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }

        RemoteFormProxy.dropCaches();

        for (ICleanListener task : cleanListeners) {
            task.clean();
        }
        cleanListeners.clear();
        eventBus.invalidate();

        frame = null;
        remoteLoader = null;
        remoteLogics = null;
        remoteNavigator = null;

        System.gc();
    }

    public static long generateID() throws RemoteException {
        return remoteLogics.generateID();
    }

    public static void main(final String[] args) {

//        SpanningTreeWithBlackjack.test();
//        SpanningTreeWithBlackjack.test1();
        start(args);
    }
}