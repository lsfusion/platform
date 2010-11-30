package platform.client;

import platform.base.OSUtils;
import platform.client.exceptions.ClientExceptionManager;
import platform.client.exceptions.ExceptionThreadGroup;
import platform.client.form.SimplexLayout;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.rmi.ConnectionLostManager;
import platform.client.rmi.RMITimeoutSocketFactory;
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
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private final static Logger logger = Logger.getLogger(SimplexLayout.class.getName());

    public static RemoteLogicsInterface remoteLogics;
    public static MainFrame frame;
    private static String[] args;
    public static int computerId;

    public static ModuleFactory module;
    public static PingThread pingThread;
    private static Thread mainThread;
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
        //рестарт в новом потоке
        new Thread(new Runnable() {
            public void run() {
                if (frame != null) {
                    frame.setVisible(false);
                    frame.dispose();
                    frame = null;
                }

                try {
                    //вызываем какойнить flush метод, чтобы обнулить lastRemoteObject в аспектах
                    remoteLogics.generateNewID();
                } catch (RemoteException ignored) {
                }
                remoteLogics = null;

                stopThread(pingThread);
                pingThread = null;
                stopThread(mainThread);
                mainThread = null;

                System.gc();

                start(args, module);
            }
        }).start();
    }

    private static void stopThread(Thread thread) {
        if (thread != null) {
            thread.interrupt();
            while (thread.getState() != Thread.State.TERMINATED) {
                thread.interrupt();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            thread = null;
        }
    }

    public static void start(final String[] iargs, ModuleFactory startModule) {
        args = iargs;
        module = startModule;

        System.setProperty("sun.awt.exception.handler", ClientExceptionManager.class.getName());

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов
        System.setProperty("sun.rmi.dgc.client.gcInterval", "60000");

        boolean turnOnRmiLogging = Boolean.getBoolean(PropertyConstants.PLATFORM_CLIENT_LOG_RMI);
        if (turnOnRmiLogging) {
            String logBaseDir = System.getProperty(PropertyConstants.PLATFORM_CLIENT_LOG_BASEDIR);
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

        mainThread = new Thread(new ExceptionThreadGroup(), "Init thread") {

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

                    String logLevel = System.getProperty(PropertyConstants.PLATFORM_CLIENT_LOGLEVEL);
                    String timeout = System.getProperty(PropertyConstants.PLATFORM_CLIENT_CONNECTION_LOST_TIMEOUT, "7200000");

                    initRMISocketFactory(timeout);

                    LoginAction loginAction = LoginAction.getDefault();
                    if (!loginAction.login()) {
                        return;
                    }

                    startSplashScreen();

                    remoteLogics = loginAction.getRemoteLogics();
                    computerId = loginAction.getComputerId();
                    RemoteNavigatorInterface remoteNavigator = loginAction.getRemoteNavigator();

                    if (logLevel != null) {
                        LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
                    } else {
                        LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
                    }

                    logger.info("Before init frame");
                    frame = module.initFrame(remoteNavigator);
                    logger.info("After init frame");

                    pingThread = new PingThread(remoteLogics, Integer.parseInt(System.getProperty("platform.client.pingTime", "1000")));
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
        };
        
        mainThread.start();
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
                SplashScreen.start();
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

    public interface ModuleFactory {
        MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException;

        void runExcel(RemoteFormInterface remoteForm);

        boolean isFull();

        SwingWorker<List<ServerInfo>, ServerInfo> getServerHostEnumerator(MutableComboBoxModel serverHostModel, String waitMessage);
    }
}