package platform.client;

import org.apache.log4j.Logger;
import platform.base.OSUtils;
import platform.client.exceptions.ClientExceptionManager;
import platform.client.exceptions.ExceptionThreadGroup;
import platform.client.form.SimplexLayout;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.rmi.ConnectionLostManager;
import platform.client.rmi.RMITimeoutSocketFactory;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;
import platform.interop.ServerInfo;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.util.List;

import static platform.client.PropertyConstants.*;

public class Main {
    private final static Logger logger = Logger.getLogger(SimplexLayout.class);

    private static final String DEFAULT_TITLE = "LS Fusion";
    private static final String DEFAULT_SPLASH_PATH = "/platform/images/lsfusion.jpg";

    public static RemoteLoaderInterface remoteLoader;
    public static RemoteLogicsInterface remoteLogics;
    public static MainFrame frame;
    public static int computerId;

    public static ModuleFactory module;
    public static PingThread pingThread;
    public static RMITimeoutSocketFactory socketFactory;

    private static ClientObjectClass baseClass = null;

    public static ClientObjectClass getBaseClass() {
        if(baseClass==null)
            try {
                baseClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(new DataInputStream(new ByteArrayInputStream(remoteLogics.getBaseClassByteArray())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        return baseClass;
    }

    public static void restart() {
        System.exit(0);
    }

    public static void start(final String[] iargs, ModuleFactory startModule) {
        module = startModule;

        System.setProperty("sun.awt.exception.handler", ClientExceptionManager.class.getName());

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов
        System.setProperty("sun.rmi.dgc.client.gcInterval", "60000");

        boolean turnOnRmiLogging = Boolean.getBoolean(PLATFORM_CLIENT_LOG_RMI);
        if (turnOnRmiLogging) {
            String logBaseDir = System.getProperty(PLATFORM_CLIENT_LOG_BASEDIR);
            if (logBaseDir != null) {
                ClientLoggingManager.turnOnRmiLogging(logBaseDir);
            } else {
                ClientLoggingManager.turnOnRmiLogging();
            }
        }

        try {
            loadLibraries();
        } catch (IOException e) {
            ClientExceptionManager.handle(e);
            throw new RuntimeException(e);
        }

        KeyboardFocusManager.setCurrentKeyboardFocusManager(new DefaultKeyboardFocusManager() {
            @Override
            protected void enqueueKeyEvents(long after, Component untilFocused) {
                super.enqueueKeyEvents(0, untilFocused);
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                ClientExceptionManager.handle(e);
            }
        });

        new Thread(new ExceptionThreadGroup(), "Init thread") {

            public void run() {

                try {

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

                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                    String timeout = System.getProperty(PLATFORM_CLIENT_CONNECTION_LOST_TIMEOUT, "7200000");

                    initRMISocketFactory(timeout);

                    LoginAction loginAction = LoginAction.getDefault();
                    if (!loginAction.login()) {
                        return;
                    }

                    remoteLogics = loginAction.getRemoteLogics();
                    computerId = loginAction.getComputerId();

                    startSplashScreen();

                    logger.info("Before init frame");
                    frame = module.initFrame(loginAction.getRemoteNavigator());
                    logger.info("After init frame");

                    pingThread = new PingThread(remoteLogics, Integer.parseInt(System.getProperty(PLATFORM_CLIENT_PINGTIME, "1000")));
                    pingThread.start();

                    frame.addWindowListener(
                            new WindowAdapter() {
                                public void windowOpened(WindowEvent e) {
                                    closeSplashScreen();
                                }

                                public void windowClosing(WindowEvent e) {
                                    try {
                                        remoteLogics.endSession(OSUtils.getLocalHostName() + " " + computerId);
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

                } catch (Exception e) {
                    closeSplashScreen();
                    throw new RuntimeException("Ошибка при инициализации приложения", e);
                }

            }
        }.start();
    }

    private static void initRMISocketFactory(String timeout) throws IOException {
        RMISocketFactory factory = RMISocketFactory.getSocketFactory();
        if (factory == null) {
            factory = RMISocketFactory.getDefaultSocketFactory();
        }

        if (socketFactory == null) {
            socketFactory = new RMITimeoutSocketFactory(factory, Integer.valueOf(timeout));

            RMISocketFactory.setFailureHandler(new RMIFailureHandler() {

                public boolean failure(Exception ex) {
                    return true;
                }
            });

            RMISocketFactory.setSocketFactory(socketFactory);
        }
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

    public static void main(final String[] args) {
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {

                String forms = System.getProperty("platform.client.forms");
                if (forms == null) {
                    String formSet = System.getProperty("platform.client.formset");
                    if (formSet == null)
                        throw new RuntimeException("Не задано свойство : -Dplatform.client.forms=formID1,formID2,... или -Dplatform.client.formset=formsetID");
                    forms = remoteNavigator.getForms(formSet);
                    if (forms == null)
                        throw new RuntimeException("На сервере не обнаружено множество форм с идентификатором " + formSet);
                }

                return new SimpleMainFrame(remoteNavigator, forms);
            }

            public void runExcel(RemoteFormInterface remoteForm) {
                // not supported
            }

            public boolean isFull() {
                return false;
            }

            public SwingWorker<List<ServerInfo>, ServerInfo> getServerHostEnumerator(MutableComboBoxModel serverHostModel, String waitMessage) {
                return null;
            }
        });
    }

    // будет загружать все не кросс-платформенные библиотеки
    private static void loadLibraries() throws IOException {
        SimplexLayout.loadLibraries();
        ComBridge.loadLibraries();
    }

    public static int generateNewID() {
        try {
            return remoteLogics.generateNewID();
        } catch (RemoteException e) {
            throw new RuntimeException("Ошибка при генерации ID");
        }
    }

    public static ImageIcon getMainIcon() {
        return getLogo();
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

        return loadResource(logoData, PLATFORM_CLIENT_LOGO, DEFAULT_SPLASH_PATH);
    }

    private static ImageIcon loadResource(byte[] logoData, String defaultUrlSystemPropName, String defaultResourcePath) {
        ImageIcon splash = logoData != null ? new ImageIcon(logoData) : null;
        if (splash == null || splash.getImageLoadStatus() != MediaTracker.COMPLETE) {
            String splashUrlString = System.getProperty(defaultUrlSystemPropName);
            URL splashUrl = null;
            if (splashUrlString != null) {
                try {
                    splashUrl = new URL(splashUrlString);
                } catch (MalformedURLException ignored) {
                }
            }
            if (splashUrl != null) {
                splash = new ImageIcon(splashUrl);
            }

            if (splash == null || splash.getImageLoadStatus() != MediaTracker.COMPLETE) {
                splash = new ImageIcon(SplashScreen.class.getResource(defaultResourcePath));
            }
        }
        return splash;
    }

    public static String getMainTitle() {
        String title = null;
        if (remoteLogics != null) {
            try {
                title = remoteLogics.getDisplayName();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return title != null ? title : DEFAULT_TITLE;
    }

    public interface ModuleFactory {
        MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException;

        void runExcel(RemoteFormInterface remoteForm);

        boolean isFull();

        SwingWorker<List<ServerInfo>, ServerInfo> getServerHostEnumerator(MutableComboBoxModel serverHostModel, String waitMessage);
    }
}