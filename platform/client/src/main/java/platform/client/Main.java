package platform.client;

import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.client.layout.Layout;
import platform.client.exceptions.ClientExceptionManager;
import platform.client.exceptions.ExceptionThreadGroup;
import platform.client.form.SimplexLayout;

import javax.swing.*;
import java.rmi.Naming;
import java.net.URL;
import java.io.IOException;

public class Main {

    public static Layout layout;

    public static void main(final String[] args) {

        try {
            loadLibraries();
        } catch (IOException e) {
            ClientExceptionManager.handleException(e);
            throw new RuntimeException(e);
        }

        new Thread(new ExceptionThreadGroup(), "Init thread") {

            public void run() {

                try {

//                    UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[2].getClassName());

                    String serverName = args.length>0?args[0]:"localhost";
                    String exportPort = args.length>1?args[1]:"7652";

//                    RemoteNavigatorInterface remoteNavigator = new LoginDialog((RemoteLogicsInterface) Naming.lookup("rmi://"+serverName+":"+exportPort+"/BusinessLogics")).login();
                    RemoteLogicsInterface remoteLogics = (RemoteLogicsInterface) Naming.lookup("rmi://" + serverName + ":" + exportPort + "/BusinessLogics");
                    RemoteNavigatorInterface remoteNavigator = remoteLogics.createNavigator("user1", "", remoteLogics.getComputers().iterator().next());
                    if (remoteNavigator == null) return;

                    layout = new Layout(remoteNavigator);
                    layout.setVisible(true);

                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при инициализации приложения", e);
                }

            }
       }.start();

    }

    // будет загружать все не кросс-платформенные библиотеки
    private static void loadLibraries() throws IOException {
        SimplexLayout.loadLibraries();
    }
}