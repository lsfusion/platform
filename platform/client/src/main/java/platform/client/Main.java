package platform.client;

import platform.base.ClassPathHacker;
import platform.base.OSUtils;
import platform.client.exceptions.ClientExceptionManager;
import platform.client.exceptions.ExceptionThreadGroup;
import platform.client.form.SimplexLayout;
import platform.interop.RemoteLogicsInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.Naming;
import java.io.IOException;
import java.rmi.server.RMIClassLoader;

public class Main {

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

                    String serverHost = System.getProperty("platform.client.hostname", "localhost");
                    String serverPort = System.getProperty("platform.client.hostport", "7652");

                    String user = System.getProperty("platform.client.user");
                    String password = System.getProperty("platform.client.password");

                    remoteLogics = (RemoteLogicsInterface) Naming.lookup("rmi://" + serverHost + ":" + serverPort + "/BusinessLogics");
                    computerId = remoteLogics.getComputer(OSUtils.getLocalHostName());

                    RemoteNavigatorInterface remoteNavigator;
                    if (user == null) {
                        remoteNavigator = new LoginDialog(computerId, remoteLogics).login();
                    } else
                        remoteNavigator = remoteLogics.createNavigator(user, password, computerId);

                    if (remoteNavigator == null) return;
                    System.out.println("Before init frame");
                    frame = module.initFrame(remoteNavigator);
                    System.out.println("After init frame");

                    frame.addWindowListener(
                            new WindowAdapter() {
                                public void windowOpened(WindowEvent e) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            SplashScreen.close();
                                        }
                                    });
                                }
                            }
                    );

                    // вот таким вот извращенным методом приходится отключать SimplexLayout, чтобы он не вызывался по два раза
                    // проблема в том, что setVisible сразу вызывает отрисовку, а setExtendedState "моделирует" нажатии кнопки ОС и все идет просто в EventDispatchThread
                    SimplexLayout.ignoreLayout = true;
                    frame.setVisible(true);
                    SimplexLayout.ignoreLayout = false;

                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    System.out.println("After setExtendedState");

                } catch (Exception e) {
//                    ClientExceptionManager.handleException(e);
                    throw new RuntimeException("Ошибка при инициализации приложения", e);
                }

            }
        }.start();
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