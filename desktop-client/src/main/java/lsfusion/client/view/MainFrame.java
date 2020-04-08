package lsfusion.client.view;

import com.google.common.base.Throwables;
import com.jhlabs.image.BlurFilter;
import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.base.SystemUtils;
import lsfusion.client.SplashScreen;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.remote.ConnectionLostManager;
import lsfusion.client.controller.remote.ReconnectWorker;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.proxy.RemoteFormProxy;
import lsfusion.client.form.print.view.EditReportInvoker;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.interop.action.ICleanListener;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.event.EventBus;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import org.apache.log4j.Logger;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static lsfusion.base.DateConverter.createDateTimeEditFormat;
import static lsfusion.base.DateConverter.createTimeEditFormat;
import static lsfusion.client.ClientResourceBundle.getString;

public abstract class MainFrame extends JFrame {
    private final static Logger logger = Logger.getLogger(MainController.class);
    
    public static MainFrame instance;
    public static void load() {
        try {
            RemoteNavigatorInterface remoteNavigator;
            try {
                remoteNavigator = new ReconnectWorker<>(new Callable<RemoteNavigatorInterface>() {
                    public RemoteNavigatorInterface call() throws Exception {
                        return RmiQueue.runRetryableRequest(new Callable<RemoteNavigatorInterface>() { // we need to retry request (at leastbecause remoteLogics ref can be invalid), just like in logicsDispatchAsync in web-client
                            public RemoteNavigatorInterface call() throws Exception {
                                try {
                                    return MainController.runRequest(new LogicsRunnable<RemoteNavigatorInterface>() {
                                        public RemoteNavigatorInterface run(LogicsSessionObject sessionObject) throws RemoteException {
                                            MainController.remoteLogics = sessionObject.remoteLogics;
                                            return MainController.remoteLogics.createNavigator(MainController.authToken, getNavigatorInfo());
                                        }
                                    });
                                } catch (AppServerNotAvailableException e) { // suppress and try again
                                    return null;
                                }
                            }
                        }, new AtomicBoolean());
                    }
                }).connect();
            } catch (AuthenticationException e) { // token is invalid, then we need to relogin (and actually need to logout, to reauthenticate and get new token) - it's the only place on client where token is checked
                MainController.authToken = null;
                MainController.authAndLoadMainFrame(e.getMessage(), null);
                return;
            } catch (RemoteMessageException e) {
                MainController.authToken = null; //see issue #312
                MainController.authAndLoadMainFrame(e.getMessage(), null);
                return;
            }

            startSplashScreen();

            ClientSettings clientSettings = remoteNavigator.getClientSettings();

            LocalePreferences userPreferences = clientSettings.localePreferences;
            fontSize = clientSettings.fontSize;
            MainController.busyDialog = clientSettings.busyDialog;
            MainController.busyDialogTimeout = Math.max(clientSettings.busyDialogTimeout, 1000); //минимальный таймаут 1000мс
            MainController.useRequestTimeout = clientSettings.useRequestTimeout;
            MainController.showDetailedInfo = clientSettings.showDetailedInfo;
            MainController.forbidDuplicateForms = clientSettings.forbidDuplicateForms;
            MainController.colorPreferences = clientSettings.colorPreferences;
            SwingDefaults.resetClientSettingsProperties();

            Locale userLocale = userPreferences.locale;
            Locale.setDefault(userLocale);
                
            UIManager.getDefaults().setDefaultLocale(userLocale);
            UIManager.getLookAndFeelDefaults().setDefaultLocale(userLocale);

            JFileChooser.setDefaultLocale(userLocale);
            JColorChooser.setDefaultLocale(userLocale);

            setupTimePreferences(userPreferences.timeZone, userPreferences.twoDigitYearStart);

            setUIFontSize();

            logger.info("Before init frame");
            DockableMainFrame frame = new DockableMainFrame(remoteNavigator, clientSettings.currentUserName);
            logger.info("After init frame");

            frame.addWindowListener(
                    new WindowAdapter() {
                        public void windowOpened(WindowEvent e) {
                            closeSplashScreen();
                        }
                    }
            );

            frame.setExtendedState(MAXIMIZED_BOTH);
            logger.info("After setExtendedState");

            ConnectionLostManager.start(frame, remoteNavigator.getClientCallBack(), clientSettings.devMode);

            frame.setVisible(true);

            frame.clearForms();

            instance = frame; // it's important to set this field before onDesktopClientStarted because it is used when getting eventbus for example
            
            MainController.changeColorTheme(clientSettings.colorTheme);

            frame.executeNavigatorAction("SystemEvents.onDesktopClientStarted[]", 0, null, null);
        } catch (Throwable e) {
            closeSplashScreen();
            logger.error(getString("client.error.application.initialization"), e);
            throw Throwables.propagate(e);
        }
    }

    // time
    
    public static DateFormat dateFormat;
    public static DateFormat dateEditFormat;
    public static DateFormat timeFormat;
    public static DateFormat timeEditFormat;
    public static DateFormat dateTimeFormat;
    public static DateFormat dateTimeEditFormat;
    public static Date wideFormattableDate;
    public static Date wideFormattableDateTime;

    private static void setupTimePreferences(String userTimeZone, Integer twoDigitYearStart) throws RemoteException {

        TimeZone timeZone = userTimeZone == null ? null : TimeZone.getTimeZone(userTimeZone);
        if (timeZone != null) {
            TimeZone.setDefault(timeZone);
        }

        Date twoDigitYearStartDate = null;
        if (twoDigitYearStart != null) {
            GregorianCalendar c = new GregorianCalendar(twoDigitYearStart, Calendar.JANUARY, 1);
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
        dateEditFormat = DateConverter.createDateEditFormat(dateFormat);
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

    public static Integer fontSize;
    private static Font defaultDefaultFont = null;
    private static void setUIFontSize() {
        if (defaultDefaultFont != null || fontSize != null) { // skip if fontSize was never set
            if (defaultDefaultFont == null) {
                defaultDefaultFont = UIManager.getFont("defaultFont");
            }

            if (defaultDefaultFont != null) {
                Font newDefaultFont;
                if (fontSize != null) {
                    newDefaultFont = new FontUIResource(defaultDefaultFont.deriveFont(getUIFontSize(defaultDefaultFont.getSize())));
                } else {
                    newDefaultFont = defaultDefaultFont;
                }
                UIManager.put("defaultFont", newDefaultFont);
            }
        }
    }

    public static float getUIFontSize(int defaultSize) {
        return fontSize != null ? (float) defaultSize * fontSize / 100 : defaultSize;
    }

    public static int getIntUISize(int defaultSize) {
        return (int) (getUIFontSize(defaultSize) + 0.5); // as deriveFont() does
    }

    private static void startSplashScreen() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SplashScreen.start(MainController.getLogo());
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

    public static NavigatorInfo getNavigatorInfo() {
        Object notClassic = Toolkit.getDefaultToolkit().getDesktopProperty("win.xpstyle.themeActive");
        String osVersion = System.getProperty("os.name") + (UIManager.getLookAndFeel().getID().equals("Windows")
                && (notClassic instanceof Boolean && !(Boolean) notClassic) ? " Classic" : "");
        String processor = System.getenv("PROCESSOR_IDENTIFIER");

        String architecture = System.getProperty("os.arch");
        if (osVersion.startsWith("Windows")) {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            architecture = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "x64" : "x32";
        }

        Integer cores = Runtime.getRuntime().availableProcessors();
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        Integer physicalMemory = (int) (os.getTotalPhysicalMemorySize() / 1048576);
        Integer totalMemory = (int) (Runtime.getRuntime().totalMemory() / 1048576);
        Integer maximumMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576);
        Integer freeMemory = (int) (Runtime.getRuntime().freeMemory() / 1048576);
        String javaVersion = SystemUtils.getJavaVersion() + " " + System.getProperty("sun.arch.data.model") + " bit";

        String screenSize = null;
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        if(dimension != null) {
            screenSize = (int) dimension.getWidth() + "x" + (int) dimension.getHeight();
        }

        return new NavigatorInfo(MainController.getSessionInfo(), osVersion, processor, architecture, cores, physicalMemory, totalMemory,
                maximumMemory, freeMemory, javaVersion, screenSize, BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion());
    }

    public ClientFormController currentForm;

    public void setCurrentForm(ClientFormController currentForm) {
        this.currentForm = currentForm;
    }

    public void dropCurrentForm(ClientFormController form) {
        if(currentForm != null && currentForm.equals(form))
            currentForm = null;
    }

    public void executeNotificationAction(final Integer idNotification) {
        try {
            if (currentForm != null)
                currentForm.executeNotificationAction(idNotification);
            else
                ((DockableMainFrame) this).executeNavigatorAction(String.valueOf(idNotification), 2, new Runnable() {
                    @Override
                    public void run() {
                        executeNotificationAction(idNotification);
                    }
                }, null);
        } catch (IOException e) {
            logger.error("Error executing notification action: ", e);
        }
    }

    public boolean clientExceptionLog(Throwable t) throws RemoteException {
        if (remoteNavigator != null) {
            remoteNavigator.logClientException(MainController.computerName, t);
            return true;
        }
        return false;
    }

    public EventBus eventBus = new EventBus();
    public List<ICleanListener> cleanListeners = new ArrayList<>();

    public void clean() {
        try {
            remoteNavigator.close();
        } catch (RemoteException e) {
        }

        RemoteFormProxy.dropCaches();

        for (ICleanListener task : cleanListeners) {
            task.clean();
        }
        cleanListeners.clear();
        eventBus.invalidate();
    }

    public interface FormCloseListener {
        void formClosed(boolean openFailed);
    }

    protected File baseDir;
    public RemoteNavigatorInterface remoteNavigator;
    public JLabel statusComponent;
    public JComponent status;

    private LockableUI lockableUI;

    public MainFrame(final RemoteNavigatorInterface remoteNavigator, String userName) throws IOException {
        super();

        this.remoteNavigator = remoteNavigator;

        setIconImages(MainController.getMainIcons());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        updateUser(userName);

        statusComponent = new JLabel();
        status = new JPanel(new BorderLayout());
        status.add(statusComponent, BorderLayout.CENTER);

        loadLayout();

        initUIHandlers();

        installLockableLayer();
    }

    private void installLockableLayer() {
    }

    public void setLocked(boolean locked) {
        lockableUI.setLocked(locked);
    }

    protected void setContent(JComponent content) {
        assert lockableUI == null;

        add(content);

        lockableUI = new LockableUI(new BufferedImageOpEffect(
                new ConvolveOp(new Kernel(1, 1, new float[]{0.5f}), ConvolveOp.EDGE_NO_OP, null), new BlurFilter()));

        JXLayer layer = new JXLayer(getContentPane(), lockableUI);
        layer.setFocusable(false);

        setContentPane(layer);
    }

    private void initUIHandlers() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                int confirmed = JOptionPane.showConfirmDialog(MainFrame.this,
                                                              getString("quit.do.you.really.want.to.quit"),
                                                              getString("quit.confirmation"),
                                                              JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    MainController.shutdown();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                //windowClosing не срабатывает, если просто вызван dispose,
                //поэтому сохраняем лэйаут в windowClosed
                saveLayout();
            }
        });
    }

    private void loadLayout() {
        baseDir = MainController.getBaseDir();

        try {
            File layoutFile = new File(baseDir, "dimension.txt");
            if (layoutFile.exists()) {
                Scanner in = new Scanner(new FileReader(layoutFile));
                int wWidth = in.nextInt();
                int wHeight = in.nextInt();
                setSize(wWidth, wHeight);
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize().getSize();
            setSize(size.width, size.height - 30);
        }
    }

    private void saveLayout() {
        baseDir.mkdirs();

        try {
            FileWriter out = new FileWriter(new File(baseDir, "dimension.txt"));

            out.write(getWidth() + " " + getHeight() + '\n');

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateUser(String userName) {
        setTitle(MainController.getMainTitle() + " - " + userName + " (" + MainController.serverInfo.host + ":" + MainController.serverInfo.port + ")");
    }

    public abstract Integer runReport(List<ReportPath> customReportPathList, String formSID, boolean isModal, ReportGenerationData generationData, String printerName) throws IOException, ClassNotFoundException;

    public Integer runReport(boolean isModal, ReportGenerationData generationData, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException {
        return runReport(isModal, generationData, null, editInvoker);
    }
    public abstract Integer runReport(boolean isModal, ReportGenerationData generationData, String printerName, EditReportInvoker editInvoker) throws IOException, ClassNotFoundException;

    public abstract ClientFormDockable runForm(String canonicalName, String formSID, boolean forbidDuplicate, RemoteFormInterface remoteForm, byte[] firstChanges, FormCloseListener closeListener);
}