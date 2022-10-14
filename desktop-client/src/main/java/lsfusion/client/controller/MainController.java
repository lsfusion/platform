package lsfusion.client.controller;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.base.classloader.RemoteClassLoader;
import lsfusion.base.file.RawFileData;
import lsfusion.base.remote.ZipClientSocketFactory;
import lsfusion.client.SingleInstance;
import lsfusion.client.StartupProperties;
import lsfusion.client.authentication.LoginDialog;
import lsfusion.client.authentication.UserInfo;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.equ.ComBridge;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.base.log.ClientLoggingManager;
import lsfusion.client.base.log.Log;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.ColorThemeChangeListener;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.remote.ConnectionLostManager;
import lsfusion.client.form.property.cell.classes.controller.rich.RichEditorPane;
import lsfusion.client.logics.LogicsProvider;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.authentication.PasswordAuthentication;
import lsfusion.interop.form.object.table.grid.user.design.ColorPreferences;
import lsfusion.interop.logics.LogicsConnection;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.SessionInfo;
import org.apache.log4j.Logger;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.client.StartupProperties.*;

public class MainController {
    private final static Logger logger = Logger.getLogger(MainController.class);

    public static final String LSFUSION_TITLE = "lsFusion";

    public static RemoteLogicsInterface remoteLogics; // hack, in theory it's better to wrap all request in runRequest, but it's not important in current usages

    // settings
    public static int asyncTimeOut;
    public static boolean busyDialog;
    public static long busyDialogTimeout;
    public static boolean useRequestTimeout;
    public static boolean showNotDefinedStrings;
    public static String matchSearchSeparator;
    public static boolean showDetailedInfo;
    public static String projectLSFDir;
    public static boolean inDevMode;
    public static boolean forbidDuplicateForms;
    public static long timeDiffServerClientLog = 1000;
    public static ColorPreferences colorPreferences;
    public static ColorTheme colorTheme = ColorTheme.DEFAULT;
    public static String userDebugPath;

    // lifecycle

    private static RemoteClassLoader remoteClassLoader;
    public static void start(final String[] args) {
        remoteClassLoader = new RemoteClassLoader(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(remoteClassLoader);

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
                ClientExceptionManager.handle(e, true);
            }
        });

        try {
            initJulLogging();

            loadLibraries();

            ZipClientSocketFactory.timeout = StartupProperties.rmiTimeout;

            initSwing();
        } catch (Exception e) {
            logger.error("Error during startup: ", e);
            e.printStackTrace();
            removeSingleInstanceListener();
            System.exit(1);
        }

        if(Boolean.parseBoolean(getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_AUTOLOGIN)))
            authToken = AuthenticationToken.ANONYMOUS;

        String serverHost = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_HOSTNAME);
        if(serverHost != null) {
            String serverPortString = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_HOSTPORT);
            int serverPort = 0;
            if (serverPortString != null) 
                serverPort = BaseUtils.nvl(Integer.parseInt(serverPortString), 7652);
            String serverDB = BaseUtils.nvl(getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_EXPORTNAME), "default");
            setServerInfo(new LogicsConnection(serverHost, serverPort, serverDB));
        }

        UserInfo userInfo = null;
        String userName = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_USER);
        String password = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_PASSWORD);
        if(userName != null)
            userInfo = new UserInfo(userName, true, password); // for command line values: set this flag to fill password field in login dialog

        final UserInfo fUserInfo = userInfo;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                authAndLoadMainFrame(null, fUserInfo);
            }
        });
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

        //даём немного времени на обработку текущих событий
        Log.log("Restart : " + reconnect);
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clean();
                //Перегружаем classLoader. Возможно, следует выполнять и другие действия из MainController.start()
                initRmiClassLoader(remoteLogics);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(!reconnect)
                            authToken = null;
                        authAndLoadMainFrame(null, null);
                    }
                });
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    // environment

    private static SingleInstance singleInstance;
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

    public static void initRmiClassLoader(RemoteLogicsInterface remoteLogics) {
        remoteClassLoader.setRemoteLogics(remoteLogics);
        // reset the SecurityManager that installs JavaWS,
        // since it doesn't let the RemoteClassLoader class do anything,
        // since it is loaded from a temporary directory
        System.setSecurityManager(null);
    }
    
    private static void initSwing() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.getDefaults().addResourceBundle("SwingResourceBundle");
        
//        FocusOwnerTracer.installFocusTracer();

        ToolTipManager.sharedInstance().setInitialDelay(1500);
        // через сколько после скрытия тултипа снова ждать Initial Delay до показа нового (не в рамках одного компонента)
        ToolTipManager.sharedInstance().setReshowDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        FlatLightLaf newLookAndFeel = new FlatLightLaf();
        UIManager.setLookAndFeel(newLookAndFeel);

        UIManager.put("Button.default.boldText", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(1, 1));
        UIManager.put("Tree.border", BorderFactory.createEmptyBorder());
        UIManager.put("TextComponent.selectAllOnFocusPolicy", "never");
        UIManager.put("Button.default.borderWidth", SwingDefaults.getButtonBorderWidth()); // differs in light and dark themes. make equal to have equal height.
        UIManager.put("TabbedPane.tabHeight", SwingDefaults.getComponentHeight());
        UIManager.put("TabbedPane.tabsPopupPolicy", "never");
        UIManager.put("ToggleButton.margin", SwingDefaults.getToggleButtonMargin());
        UIManager.put("SplitPaneDivider.style", "plain");

        setUIDefaults();
        
        // при первом использовании rich-editora во время редактирования, его создание тормозит...
        // возможно, где-то внутри кэшируются какие-то lazy-ресурсы... Чтобы это не напрягало на форме, создаём компонент вхолостую здесь
        new RichEditorPane();
    }

    public static void setStatusText(String msg) {
        if (MainFrame.instance != null) {
            MainFrame.instance.statusComponent.setText(msg);
        }
    }

    public static long getBytesSent() {
        return ZipClientSocketFactory.outSum;
    }

    public static long getBytesReceived() {
        return ZipClientSocketFactory.inSum;
    }

    // edit reports
    
    public static void addReportPathList(List<String> reportPathList, String formSID) throws IOException {
        reportPathList.addAll(MainController.remoteLogics.saveAndGetCustomReportPathList(formSID, false));
        editReportPathList(reportPathList);
    }
    public static void recreateReportPathList(List<String> reportPathList, String formSID) throws IOException {
        MainController.remoteLogics.saveAndGetCustomReportPathList(formSID, true);
        editReportPathList(reportPathList);
    }
    public static void editReportPathList(List<String> reportPathList) throws IOException {
        for (String reportPath : reportPathList) {
            Desktop.getDesktop().open(new File(reportPath));
        }
    }

    public static void deleteReportPathList(List<String> reportPathList) {
        for (String reportPath : reportPathList) {
            File customFile = new File(reportPath);
            if(!customFile.delete())
                customFile.deleteOnExit();
        }
        reportPathList.clear();
    }

    private static void clean() {
        
        Log.log("Clean");
        
        if (MainFrame.instance != null) {
            MainFrame cleanFrame = MainFrame.instance;
            MainFrame.instance = null;

            cleanFrame.clean();
            cleanFrame.setVisible(false);
            cleanFrame.dispose();            
        }

        remoteLogics = null;

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

    public static String computerName;
    public static SessionInfo getSessionInfo() {
        return new SessionInfo(computerName, SystemUtils.getLocalHostIP(), Locale.getDefault().getLanguage(), Locale.getDefault().getCountry(),
                BaseUtils.getDatePattern(), BaseUtils.getTimePattern());
    }

    public static LogicsConnection serverInfo;

    public static void setServerInfo(LogicsConnection serverInfo) {
        MainController.serverInfo = serverInfo;

        serverSettings = LogicsProvider.instance.getServerSettings(serverInfo, getSessionInfo(), null, false);
    }

    public static AuthenticationToken authToken;

    public static void authAndLoadMainFrame(String warningMsg, UserInfo userInfo) {
        if(authToken == null) {
            if(!loginAndAuthenticateUser(warningMsg, userInfo))
                return;
        } else
            assert serverInfo != null;

        MainFrame.load();
    }

    public static boolean loginAndAuthenticateUser(String warningMsg, UserInfo userInfo) {
        if(serverInfo == null || userInfo == null) { // if there is no user or server, ask user
            Pair<LogicsConnection, UserInfo> serverAndUserInfo = LoginDialog.login(serverInfo, userInfo, warningMsg);
            if(serverAndUserInfo == null)
                return false;
            
            setServerInfo(serverAndUserInfo.first);
            userInfo = serverAndUserInfo.second;
        }
        try {
            assert authToken == null;
            final UserInfo fUserInfo = userInfo;
            authToken = runRequest((sessionObject, retry) -> fUserInfo.isAnonymous() ? AuthenticationToken.ANONYMOUS : sessionObject.remoteLogics.authenticateUser(new PasswordAuthentication(fUserInfo.name, fUserInfo.password)));
        } catch (Exception e) {
            return loginAndAuthenticateUser(e.getMessage(), null);
        }
        
        return true;
    }
    
    public static <T> T runRequest(LogicsRunnable<T> runnable) throws AppServerNotAvailableException, RemoteException {
        return LogicsProvider.instance.runRequest(serverInfo, runnable);
    }

    // server settings
    
    private static ServerSettings serverSettings;

    public static String getMainTitle(ServerSettings settings) {
        return nvl(BaseUtils.nullEmpty(settings != null ? settings.displayName : null), LSFUSION_TITLE);
    }

    private static final String DEFAULT_ICON_PATH = "logo/";
    public static List<Image> getMainIcons(ServerSettings serverSettings) {
        Set<Image> images = new LinkedHashSet<>();
        RawFileData logicsMainIcon = serverSettings != null ? serverSettings.logicsIcon : null;
        if(logicsMainIcon != null) {
            ImageIcon resource = logicsMainIcon.getImageIcon();
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

    public static ImageIcon getLogo(ServerSettings serverSettings) {
        RawFileData logicsMainLogo = serverSettings != null ? serverSettings.logicsLogo : null;
        return logicsMainLogo != null ? logicsMainLogo.getImageIcon() : getImageIcon(DEFAULT_ICON_PATH + "logo.png");
    }

    private static Image getImage(String resourcePath) {
        return getImageIcon(resourcePath).getImage();
    }

    private static ImageIcon getImageIcon(String resourcePath) {
        return ClientImages.readImage(resourcePath);
    }

    public static String getMainTitle() {
        return getMainTitle(serverSettings);
    }
    public static List<Image> getMainIcons() {
        return getMainIcons(serverSettings);
    }
    public static ImageIcon getLogo() {
        return getLogo(serverSettings);
    }

    public static final File fusionDir = new File(System.getProperty("user.home"), ".fusion");
    public static File getBaseDir() {
        File baseDir;
        String logicsName = serverSettings != null ? serverSettings.logicsName : null;
        if (logicsName != null) {
            baseDir = new File(fusionDir, logicsName);
        } else {
            baseDir = fusionDir;
        }
        return baseDir;
    }

    public static String getHelpTitle() {
        String text = serverSettings != null ? serverSettings.displayName : null;
        if (text == null || text.isEmpty()) {
            text = LSFUSION_TITLE;
        } else {
            text = "<html><b>" + text + "</b> powered by " + LSFUSION_TITLE + "</html>";
        }
        return text;
    }

    public static Set<ColorThemeChangeListener> colorThemeChangeListeners = Collections.newSetFromMap(new WeakHashMap<>());
    public static void addColorThemeChangeListener(ColorThemeChangeListener listener) {
        colorThemeChangeListeners.add(listener);
    }
    
    // properties which depend on current color theme. 
    public static void setUIDefaults() {
        UIManager.put("TableHeader.background", SwingDefaults.getColor("Panel.background"));
        UIManager.put("Table.cellFocusColor", SwingDefaults.getSelectionBorderColor()); // mostly for tree table. has no effect as tree has no focus 
        UIManager.put("TitledBorder.titleColor", SwingDefaults.getTitledBorderTitleColor());
        
        UIManager.put("TabbedPane.underlineColor", SwingDefaults.getTabbedPaneUnderlineColor());
        UIManager.put("TabbedPane.hoverColor", SwingDefaults.getToggleButtonHoverBackground());
        UIManager.put("TabbedPane.focusColor", SwingDefaults.getTabbedPaneFocusColor());
        
        UIManager.put("ToggleButton.selectedBackground", SwingDefaults.getSelectionColor());
        UIManager.put("ToggleButton.pressedBackground", SwingDefaults.getToggleButtonPressedBackground());

        UIManager.put("Component.focusedBorderColor", SwingDefaults.getComponentFocusBorderColor());
        UIManager.put("Button.default.focusedBorderColor", SwingDefaults.getComponentFocusBorderColor());
        UIManager.put("Button.focusedBorderColor", SwingDefaults.getComponentFocusBorderColor());
        UIManager.put("CheckBox.icon.focusedBorderColor", SwingDefaults.getComponentFocusBorderColor());

        UIManager.put("Button.default.hoverBorderColor", SwingDefaults.getComponentFocusBorderColor());
        UIManager.put("Button.hoverBorderColor", SwingDefaults.getComponentFocusBorderColor());
        UIManager.put("CheckBox.icon.hoverBorderColor", SwingDefaults.getComponentFocusBorderColor());

        UIManager.put("Button.default.focusedBackground", SwingDefaults.getSelectionColor());
        UIManager.put("Button.focusedBackground", SwingDefaults.getSelectionColor());
        
        UIManager.put("Button.default.hoverBackground", SwingDefaults.getButtonHoverBackground());
        UIManager.put("Button.hoverBackground", SwingDefaults.getButtonHoverBackground());
        UIManager.put("Button.default.pressedBackground", SwingDefaults.getButtonPressedBackground());
        UIManager.put("Button.pressedBackground", SwingDefaults.getButtonPressedBackground());

        setClientSettingsDependentUIDefaults();
    }
    
    // properties which depend on client settings and are stored in UIDefaults (not read in runtime)
    public static void setClientSettingsDependentUIDefaults() {
        UIManager.put("Table.gridColor", SwingDefaults.getTableGridColor()); // Actually doesn't fully work. We have to update gridColor in JTable's updateUI() for existing tables
        UIManager.put("TableHeader.separatorColor", SwingDefaults.getTableGridColor());
        UIManager.put("TableHeader.bottomSeparatorColor", SwingDefaults.getTableGridColor());
    }

    public static void changeColorTheme(ColorTheme newColorTheme) {
        if (colorTheme != newColorTheme) {
            colorTheme = newColorTheme;

            FlatLaf newLookAndFeel = colorTheme.isLight() ? new FlatLightLaf() : new FlatDarkLaf();
            
            ClientImages.reset();
            SwingDefaults.reset();
            setUIDefaults();
            
            FlatLaf.setup(newLookAndFeel);
            FlatLaf.updateUI();

            for (ColorThemeChangeListener colorThemeChangeListener : new HashSet<>(colorThemeChangeListeners)) {
                colorThemeChangeListener.colorThemeChanged();
            }
        }
    }
}