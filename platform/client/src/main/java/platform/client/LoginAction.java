package platform.client;

import platform.base.OSUtils;
import platform.client.remote.proxy.RemoteBusinessLogicProxy;
import platform.client.rmi.ConnectionLostManager;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.ClientCallbackInterface;

import javax.swing.*;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public final class LoginAction {

    public String serverHost;
    private String serverPort;
    private String user;
    private String password;
    private LoginInfo loginInfo;
    private LoginDialog loginDialog;

    private RemoteLogicsInterface remoteLogics;
    private int computerId;
    private RemoteNavigatorInterface remoteNavigator;
    final static int OK = 0;
    final static int HOST_NAME_ERROR = 1;
    final static int CONNECT_ERROR = 2;
    final static int SERVER_ERROR = 3;
    final static int ERROR = 4;

    private ClientCallBack clientCallBack;

    private LoginAction() {
        this.serverHost = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTNAME);
        this.serverPort = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTPORT);
        this.user = System.getProperty(PropertyConstants.PLATFORM_CLIENT_USER);
        this.password = System.getProperty(PropertyConstants.PLATFORM_CLIENT_PASSWORD);
        this.loginInfo = new LoginInfo(serverHost, serverPort, user, password);

        try {
            clientCallBack = new ClientCallBack();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static class LoginActionHolder {
        private static LoginAction instance = new LoginAction();
    }

    public static LoginAction getDefault() {
        return LoginActionHolder.instance;
    }

    public boolean login() throws MalformedURLException, NotBoundException, RemoteException {
        if (loginDialog == null) {
            loginDialog = new LoginDialog(loginInfo);
        }

        boolean needData = serverHost == null || serverPort == null || user == null || password == null;
        boolean autoLogin = Boolean.getBoolean(PropertyConstants.PLATFORM_CLIENT_AUTOLOGIN);
        if (!autoLogin || needData) {
            loginDialog.setAutoLogin(autoLogin);
            loginInfo = loginDialog.login();
        }
        if (loginInfo == null) {
            return false;
        }

        int status = connect();
        while (!(status == OK)) {
            switch (status) {
                case HOST_NAME_ERROR:
                    loginDialog.setWarningMsg("Проверьте адрес сервера.");
                    break;
                case CONNECT_ERROR:
                    loginDialog.setWarningMsg("Ошибка подключения к серверу.");
                    break;
                case SERVER_ERROR:
                    loginDialog.setWarningMsg("Проверьте имя пользователя и пароль.");
                    break;
                case ERROR:
                    loginDialog.setWarningMsg("Ошибка подключения.");
                    break;
            }
            loginDialog.setAutoLogin(false);
            loginInfo = loginDialog.login();
            if (loginInfo == null) {
                return false;
            }
            status = connect();
        }

        return true;
    }

    private int connect() {
        RemoteLoaderInterface remoteLoader;
        RemoteLogicsInterface remoteLogics;
        int computerId;
        RemoteNavigatorInterface remoteNavigator;

        try {
            Main.remoteLoader = remoteLoader = (RemoteLoaderInterface) Naming.lookup("rmi://" + loginInfo.getServerHost() + ":" + loginInfo.getServerPort() + "/BusinessLogicsLoader");
            RemoteLogicsInterface remote = remoteLoader.getRemoteLogics();

            remoteLogics = new RemoteBusinessLogicProxy(remote);
            computerId = remoteLogics.getComputer(OSUtils.getLocalHostName());
            remoteNavigator = remoteLogics.createNavigator(clientCallBack, loginInfo.getUserName(), loginInfo.getPassword(), computerId);
        } catch (UnknownHostException e) {
            System.out.println(e.getCause());
            return HOST_NAME_ERROR;
        } catch (ConnectException e) {
            System.out.println(e.getCause());
            return CONNECT_ERROR;
        } catch (ServerException e) {
            e.printStackTrace();
            return SERVER_ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }

        this.remoteLogics = remoteLogics;
        this.computerId = computerId;
        this.remoteNavigator = remoteNavigator;

        return OK;
    }

    public RemoteLogicsInterface getRemoteLogics() {
        return remoteLogics;
    }

    public int getComputerId() {
        return computerId;
    }

    public RemoteNavigatorInterface getRemoteNavigator() {
        return remoteNavigator;
    }

    public static class ClientCallBack extends UnicastRemoteObject implements ClientCallbackInterface {
        public ClientCallBack() throws RemoteException {
        }

        public void disconnect() throws RemoteException {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ConnectionLostManager.forceDisconnect();
                }
            });
        }
    }
}
