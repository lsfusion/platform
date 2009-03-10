package platform.client;

import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.client.layout.Layout;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.io.IOException;

public class Main {

    public static Layout layout;

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, UnsupportedLookAndFeelException, MalformedURLException, NotBoundException {

        UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[2].getClassName());

        String serverName = args.length>0?args[0]:"127.0.0.1";

//        RemoteNavigatorInterface remoteNavigator = new LoginDialog((RemoteLogicsInterface) Naming.lookup("rmi://"+serverName+":7653/TmcBusinessLogics")).login();
        RemoteNavigatorInterface remoteNavigator = ((RemoteLogicsInterface) Naming.lookup("rmi://"+serverName+":7653/TmcBusinessLogics"))
                .createNavigator("user1", "user1");
        if (remoteNavigator == null) return;
        
        layout = new Layout(remoteNavigator);
        layout.setVisible(true);
    }
}