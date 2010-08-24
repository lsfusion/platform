package platform.client;

import platform.base.OSUtils;
import platform.client.exceptions.ClientExceptionManager;
import platform.client.exceptions.ExceptionThreadGroup;
import platform.client.form.SimplexLayout;
import platform.client.remote.proxy.RemoteBusinessLogicProxy;
import platform.client.rmi.ConnectionLostManager;
import platform.client.rmi.RMITimeoutSocketFactory;
import platform.interop.RemoteLogicsInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.Naming;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private final static Logger logger = Logger.getLogger(SimplexLayout.class.getName());
    public static MainFrame frame;

    public interface ModuleFactory {
        MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException;

        void runExcel(RemoteFormInterface remoteForm);

        boolean isFull();
    }

    public static RemoteLogicsInterface remoteLogics;
    public static int computerId;

    public static ModuleFactory module;

    public static void start(final String[] args, ModuleFactory startModule) {
        module = startModule;

        try {
            loadLibraries();
        } catch (IOException e) {
            ClientExceptionManager.handleException(e);
            throw new RuntimeException(e);
        }

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

                    String serverHost = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTNAME);
                    String serverPort = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTPORT);
                    String user       = System.getProperty(PropertyConstants.PLATFORM_CLIENT_USER);
                    String password   = System.getProperty(PropertyConstants.PLATFORM_CLIENT_PASSWORD);
                    String logLevel   = System.getProperty(PropertyConstants.PLATFORM_CLIENT_LOGLEVEL);
                    String timeout    = System.getProperty(PropertyConstants.PLATFORM_CLIENT_CONNECTION_LOST_TIMEOUT, "3000");

                    LoginInfo loginInfo = new LoginInfo(serverHost, serverPort, user, password);
                    boolean needData = serverHost == null || serverPort == null || user == null || password == null;
                    if (!Boolean.getBoolean(PropertyConstants.PLATFORM_CLIENT_AUTOLOGIN) || needData) {
                        loginInfo = new LoginDialog(loginInfo).login();
                    }
                    if (loginInfo == null) {
                        return;
                    }

                    startSplashScreen();

                    RMISocketFactory socketFactory = RMISocketFactory.getSocketFactory();
                    if (socketFactory == null) {
                        socketFactory = RMISocketFactory.getDefaultSocketFactory();
                    }

                    socketFactory = new RMITimeoutSocketFactory(socketFactory, Integer.valueOf(timeout));

                    RMISocketFactory.setFailureHandler(new RMIFailureHandler() {

                        public boolean failure(Exception ex) {
                            return true;
                        }
                    });

                    RMISocketFactory.setSocketFactory(socketFactory);

                    RemoteLogicsInterface remote = (RemoteLogicsInterface) Naming.lookup("rmi://" + loginInfo.getServerHost() + ":" + loginInfo.getServerPort() + "/BusinessLogics");
                    remoteLogics = new RemoteBusinessLogicProxy(remote);

                    computerId = remoteLogics.getComputer(OSUtils.getLocalHostName());

                    RemoteNavigatorInterface remoteNavigator = remoteLogics.createNavigator(loginInfo.getUserName(), loginInfo.getPassword(), computerId);

                    if (logLevel != null) {
                        LogManager.getLogManager().getLogger("").setLevel(Level.parse(logLevel));
                    } else {
                        LogManager.getLogManager().getLogger("").setLevel(Level.SEVERE);
                    }

                    logger.info("Before init frame");
                    frame = module.initFrame(remoteNavigator);
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

                    ConnectionLostManager.install(frame);

                    frame.setVisible(true);

                } catch (Exception e) {
                    closeSplashScreen();
                    throw new RuntimeException("Ошибка при инициализации приложения", e);
                }

            }
        }.start();
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
        });
    }

    // будет загружать все не кросс-платформенные библиотеки
    private static void loadLibraries() throws IOException {
        SimplexLayout.loadLibraries();
        ComBridge.loadLibraries();
    }
}