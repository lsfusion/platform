package platform.client;

import platform.client.exceptions.ClientExceptionManager;
import platform.client.exceptions.ExceptionThreadGroup;
import platform.client.form.SimplexLayout;
import platform.interop.RemoteLogicsInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.io.IOException;
import java.rmi.Naming;

public class Main {

    public static MainFrame frame;

    public interface ModuleFactory {
        MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException;

        void runExcel(RemoteFormInterface remoteForm);

        boolean isFull();
    }

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

                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                    String serverName = args.length>0?args[0]:"localhost";
                    String exportPort = args.length>1?args[1]:"7652";

//                    RemoteNavigatorInterface remoteNavigator = new LoginDialog((RemoteLogicsInterface) Naming.lookup("rmi://"+serverName+":"+exportPort+"/BusinessLogics")).login();
                    RemoteLogicsInterface remoteLogics = (RemoteLogicsInterface) Naming.lookup("rmi://" + serverName + ":" + exportPort + "/BusinessLogics");
                    RemoteNavigatorInterface remoteNavigator = remoteLogics.createNavigator("user1", "", remoteLogics.getComputers().iterator().next());
                    if (remoteNavigator == null) return;

                    frame = module.initFrame(remoteNavigator);

                    // вот таким вот извращенным методом приходится отключать SimplexLayout, чтобы он не вызывался по два раза
                    // проблема в том, что setVisible сразу вызывает отрисовку, а setExtendedState "моделирует" нажатии кнопки ОС и все идет просто в EventDispatchThread
                    SimplexLayout.ignoreLayout = true;
                    frame.setVisible(true);
                    SimplexLayout.ignoreLayout = false;

                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);

                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при инициализации приложения", e);
                }

            }
       }.start();
    }

    public static void main(final String[] args) {
        start(args, new ModuleFactory() {
            public MainFrame initFrame(RemoteNavigatorInterface remoteNavigator) throws ClassNotFoundException, IOException {
                String forms = System.getProperty("platform.client.forms");
                if(forms==null)
                    throw new RuntimeException("Не задано свойство : -Dplatform.client.forms=formID1,formID2,... ");
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
    }
}