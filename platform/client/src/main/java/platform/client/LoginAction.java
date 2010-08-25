package platform.client;

import platform.base.OSUtils;
import platform.client.remote.proxy.RemoteBusinessLogicProxy;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class LoginAction {

    private String serverHost;
    private String serverPort;
    private String user;
    private String password;
    private LoginInfo loginInfo;
    private LoginDialog loginDialog;
    
    private RemoteLogicsInterface remoteLogics;
    private int computerId;
    private RemoteNavigatorInterface remoteNavigator;

    private LoginAction() {
        this.serverHost = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTNAME);
        this.serverPort = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTPORT);
        this.user       = System.getProperty(PropertyConstants.PLATFORM_CLIENT_USER);
        this.password   = System.getProperty(PropertyConstants.PLATFORM_CLIENT_PASSWORD);
        this.loginInfo  = new LoginInfo(serverHost, serverPort, user, password);
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

        while (!connect()) {
            loginDialog.setWarningMsg("Проверьте имя пользователя и пароль.");
            loginDialog.setAutoLogin(false);
            loginInfo = loginDialog.login();
            if (loginInfo == null) {
                return false;
            }
        }

        return true;
    }

    private boolean connect() {
        RemoteLogicsInterface remoteLogics;
        int computerId;
        RemoteNavigatorInterface remoteNavigator;

        try {
            RemoteLogicsInterface remote = (RemoteLogicsInterface) Naming.lookup("rmi://" + loginInfo.getServerHost() + ":" + loginInfo.getServerPort() + "/BusinessLogics");
            remoteLogics = new RemoteBusinessLogicProxy(remote);
            computerId = remoteLogics.getComputer(OSUtils.getLocalHostName());
            remoteNavigator = remoteLogics.createNavigator(loginInfo.getUserName(), loginInfo.getPassword(), computerId);
        } catch (Exception e) {
            return false;
        }

        this.remoteLogics = remoteLogics;
        this.computerId = computerId;
        this.remoteNavigator = remoteNavigator;

        return true;
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
}
