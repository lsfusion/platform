package platform.client;

import platform.base.OSUtils;
import platform.client.remote.proxy.RemoteBusinessLogicProxy;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;
import platform.interop.exceptions.InternalServerException;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.net.MalformedURLException;
import java.rmi.*;

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
    final static int PENDING_RESTART_WARNING = 4;
    final static int ERROR = 5;

    private LoginAction() {
        this.serverHost = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTNAME);
        this.serverPort = System.getProperty(PropertyConstants.PLATFORM_CLIENT_HOSTPORT);
        this.user = System.getProperty(PropertyConstants.PLATFORM_CLIENT_USER);
        this.password = System.getProperty(PropertyConstants.PLATFORM_CLIENT_PASSWORD);
        this.loginInfo = new LoginInfo(serverHost, serverPort, user, password);
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
                    loginDialog.setWarningMsg(ClientResourceBundle.getString("errors.check.server.address"));
                    break;
                case CONNECT_ERROR:
                    loginDialog.setWarningMsg(ClientResourceBundle.getString("errors.error.connecting.to.the.server"));
                    break;
                case SERVER_ERROR:
                    loginDialog.setWarningMsg(ClientResourceBundle.getString("errors.check.login.and.password"));
                    break;
                case PENDING_RESTART_WARNING:
                    loginDialog.setWarningMsg(ClientResourceBundle.getString("errors.server.reboots"));
                    break;
                case ERROR:
                    loginDialog.setWarningMsg(ClientResourceBundle.getString("errors.error.connecting"));
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

            remoteNavigator = remoteLogics.createNavigator(loginInfo.getUserName(), loginInfo.getPassword(), computerId);
            if (remoteNavigator == null) {
                Main.remoteLoader = null;
                return PENDING_RESTART_WARNING;
            }
        } catch (UnknownHostException e) {
            System.out.println(e.getCause());
            return HOST_NAME_ERROR;
        } catch (ConnectException e) {
            System.out.println(e.getCause());
            return CONNECT_ERROR;
        } catch (InternalServerException e) {
            e.printStackTrace();
            return SERVER_ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }

        this.remoteLogics = remoteLogics;
        this.remoteNavigator = remoteNavigator;
        this.computerId = computerId;

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

}
