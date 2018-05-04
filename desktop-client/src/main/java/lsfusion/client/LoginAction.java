package lsfusion.client;

import lsfusion.base.BaseUtils;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.SystemUtils;
import lsfusion.client.remote.proxy.RemoteBusinessLogicProxy;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.RemoteLogicsLoaderInterface;
import lsfusion.interop.exceptions.LockedException;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteInternalException;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import org.apache.commons.codec.binary.Base64;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CancellationException;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.*;

public final class LoginAction {
    private static final String CONFIG_FILE_NAME = "login.dialog.cfg";
    
    private static class LoginActionHolder {
        private static LoginAction instance = new LoginAction();
    }

    public static LoginAction getInstance() {
        return LoginActionHolder.instance;
    }

    //login statuses
    final static int OK = 0;
    final static int HOST_NAME_ERROR = 1;
    final static int CONNECT_ERROR = 2;
    final static int SERVER_ERROR = 3;
    final static int PENDING_RESTART_WARNING = 4;
    final static int ERROR = 5;
    final static int CANCELED = 6;
    final static int LOGIN_ERROR = 7;
    final static int LOCKED_ERROR = 8;

    private boolean autoLogin;
    public LoginInfo loginInfo;
    private LoginDialog loginDialog;
    private List<UserInfo> userInfos = new ArrayList<>();

    private RemoteLogicsInterface remoteLogics;
    private long computerId;
    private RemoteNavigatorInterface remoteNavigator;

    private LoginAction() {
        autoLogin = Boolean.parseBoolean(getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_AUTOLOGIN));
        String serverHost = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_HOSTNAME);
        String serverPort = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_HOSTPORT);
        String serverDB = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_EXPORTNAME);
        String userName = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_USER);
        String password = getSystemPropertyWithJNLPFallback(LSFUSION_CLIENT_PASSWORD);
        loginInfo = restoreLoginData(new LoginInfo(serverHost, serverPort, serverDB, userName, password, false));

        //loginDialog = new LoginDialog(loginInfo);
    }

    public void initLoginDialog() {
        loginDialog = new LoginDialog(loginInfo, userInfos);
    }

    private void storeServerData() {
        try {
            FileWriter fileWr = new FileWriter(SystemUtils.getUserFile(CONFIG_FILE_NAME));
            fileWr.write(loginInfo.getServerHost() + '\n');
            fileWr.write(loginInfo.getServerPort() + '\n');
            // всё в одной строке для упрощения поддержки обратной совместимости при добавлении функционала по сохранению всех паролей  
            // вообще здесь уже просится что-то типа XML
            List<UserInfo> newUserList = new ArrayList<>();
            UserInfo currentUserInfo = new UserInfo(loginInfo.getUserName(), loginInfo.getSavePwd(), loginInfo.getPassword());
            newUserList.add(currentUserInfo);
            StringBuilder usersString = new StringBuilder(currentUserInfo.toString());
            for (UserInfo userInfo : userInfos) {
                if (!userInfo.name.equals(loginInfo.getUserName())) {
                    usersString.append("\t").append(userInfo);
                    newUserList.add(userInfo);
                }
            }
            userInfos = newUserList;
            fileWr.write(usersString + "\n");
            fileWr.write(loginInfo.getServerDB() + '\n');
            fileWr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LoginInfo restoreLoginData(LoginInfo loginInfo) {
        try {
            File file = SystemUtils.getUserFile(CONFIG_FILE_NAME, false);
            if (file.exists()) {
                FileReader fileRd = new FileReader(file);
                Scanner scanner = new Scanner(fileRd);
                String serverHost = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getServerHost() != null) {
                    serverHost = loginInfo.getServerHost();
                }
                String serverPort = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getServerPort() != null) {
                    serverPort = loginInfo.getServerPort();
                }
                boolean newScheme = true; // для поддержки обратной совместимости. делалось на скорую руку. через некоторое время следует как минимум удалить ветку с !newScheme
                userInfos = new ArrayList<>();
                if (scanner.hasNextLine()) {
                    String users = scanner.nextLine();
                    if (!users.isEmpty()) {
                        Scanner usersScanner = new Scanner(users);
                        while (usersScanner.hasNext()) {
                            String name = usersScanner.next();
                            boolean save = false;
                            String pass = null;
                            if (usersScanner.hasNext()) {
                                String saveString = usersScanner.next();
                                if ("true".equals(saveString) || "false".equals(saveString)) {
                                    save = Boolean.parseBoolean(saveString);
                                } else {
                                    newScheme = false;
                                    break;
                                }
                            }
                            if (save && usersScanner.hasNext()) {
                                pass = new String(Base64.decodeBase64(usersScanner.next()));
                            }
                            userInfos.add(new UserInfo(name, save, pass));
                        }
                        
                        if (!newScheme) {
                            String[] userStrings = users.split("\t");
                            for (String userString : userStrings) {
                                userInfos.add(new UserInfo(userString, false, null));
                            }
                        }
                    }
                }
                String userName = loginInfo.getUserName();
                if (userName == null && !userInfos.isEmpty()){
                    userName = userInfos.get(0).name;
                }
                String serverDB = scanner.hasNextLine() ? scanner.nextLine() : "";
                if (loginInfo.getServerDB() != null) {
                    serverDB = loginInfo.getServerDB();
                }
                if (serverDB.isEmpty()) {
                    serverDB = "default";
                }
                boolean savePwd = false;
                if (newScheme) {
                    if (!userInfos.isEmpty()) {
                        savePwd = userInfos.get(0).savePassword;
                    }
                } else {
                    savePwd = Boolean.valueOf(scanner.hasNextLine() ? scanner.nextLine() : "");
                }
                String password = "";
                if (loginInfo.getPassword() != null) {
                    password = loginInfo.getPassword();
                } else if (newScheme) {
                    if (!userInfos.isEmpty()) {
                        password = userInfos.get(0).password;
                    }
                } else if (scanner.hasNextLine()) {
                    password = new String(Base64.decodeBase64(scanner.nextLine()));
                }
                return new LoginInfo(serverHost, serverPort, serverDB, userName, password, savePwd);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return loginInfo;
        }

        return loginInfo;
    }

    public String getSystemPropertyWithJNLPFallback(String propertyName) {
        String value = System.getProperty(propertyName);
        return value != null ? value : System.getProperty("jnlp." + propertyName);
    }

    public boolean login() throws MalformedURLException, NotBoundException, RemoteException {
        boolean needData = loginInfo.getServerHost() == null || loginInfo.getServerPort() == null || loginInfo.getServerDB() == null || loginInfo.getUserName() == null || loginInfo.getPassword() == null;
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
                    loginDialog.setWarningMsg(getString("errors.check.server.address"));
                    break;
                case CONNECT_ERROR:
                    loginDialog.setWarningMsg(getString("errors.error.connecting.to.the.server"));
                    break;
                case SERVER_ERROR:
                    loginDialog.setWarningMsg(getString("errors.internal.server.error"));
                    break;
                case PENDING_RESTART_WARNING:
                    loginDialog.setWarningMsg(getString("errors.server.reboots"));
                    break;
                case ERROR:
                    loginDialog.setWarningMsg(getString("errors.error.connecting"));
                    break;
                case CANCELED:
                    loginDialog.setWarningMsg(getString("errors.error.cancel"));
                    break;
                case LOGIN_ERROR:
                    loginDialog.setWarningMsg(getString("errors.check.login.and.password"));
                    break;
                case LOCKED_ERROR:
                    loginDialog.setWarningMsg(getString("errors.locked.user"));
                    break;
            }
            loginDialog.setAutoLogin(false);
            loginInfo = loginDialog.login();
            if (loginInfo == null) {
                return false;
            }
            status = connect();
        }
        
        storeServerData();

        return true;
    }

    private int connect() {
        RemoteLogicsLoaderInterface remoteLoader;
        RemoteLogicsInterface remoteLogics;
        long computerId;
        RemoteNavigatorInterface remoteNavigator;

        try {
            //Нужно сразу инициализировать Main.remoteLoader, т.к. используется для загрузки классов в ClientRMIClassLoaderSpi
            Main.remoteLoader = remoteLoader = new ReconnectWorker(loginInfo.getServerHost(), loginInfo.getServerPort(), loginInfo.getServerDB()).connect(true);
            if (remoteLoader == null) {
                return CANCELED;
            }
            RemoteLogicsInterface remote = remoteLoader.getLogics();

            remoteLogics = new RemoteBusinessLogicProxy(remote);
            computerId = remoteLogics.getComputer(SystemUtils.getLocalHostName());

            Object notClassic = Toolkit.getDefaultToolkit().getDesktopProperty("win.xpstyle.themeActive");
            String osVersion = System.getProperty("os.name") + (UIManager.getLookAndFeel().getID().equals("Windows")
                    && (notClassic instanceof Boolean && !(Boolean) notClassic) ? " Classic" : "");
            String processor = System.getenv("PROCESSOR_IDENTIFIER");

            String architecture = System.getProperty("os.arch");
            if (osVersion.startsWith("Windows")) {
                String arch = System.getenv("PROCESSOR_ARCHITECTURE");
                String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
                architecture = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? "x64" : "x32";
            }

            Integer cores = Runtime.getRuntime().availableProcessors();
            com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            Integer physicalMemory = (int) (os.getTotalPhysicalMemorySize() / 1048576);
            Integer totalMemory = (int) (Runtime.getRuntime().totalMemory() / 1048576);
            Integer maximumMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576);
            Integer freeMemory = (int) (Runtime.getRuntime().freeMemory() / 1048576);
            String javaVersion = SystemUtils.getJavaVersion() + " " + System.getProperty("sun.arch.data.model") + " bit";
            
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            
            String screenSize = null;
            Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
            if(dimension != null) {
                screenSize = (int) dimension.getWidth() + "x" + (int) dimension.getHeight();
            }

            remoteNavigator = remoteLogics.createNavigator(Main.module.isFull(), new NavigatorInfo(loginInfo.getUserName(),
                    loginInfo.getPassword(), computerId, SystemUtils.getLocalHostIP(), osVersion, processor, architecture,
                    cores, physicalMemory, totalMemory, maximumMemory, freeMemory, javaVersion, screenSize, language, country), true);
            if (remoteNavigator == null) {
                Main.remoteLoader = null;
                return PENDING_RESTART_WARNING;
            }
        } catch (CancellationException ce) {
            return CANCELED;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return HOST_NAME_ERROR;
        } catch (RemoteInternalException e) {
            e.printStackTrace();
            return SERVER_ERROR;
        } catch (LoginException e) {
            e.printStackTrace();
            return LOGIN_ERROR;
        } catch (LockedException e) {
            e.printStackTrace();
            return LOCKED_ERROR;
        } catch (Throwable e) {
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

    public long getComputerId() {
        return computerId;
    }

    public RemoteNavigatorInterface getRemoteNavigator() {
        return remoteNavigator;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public boolean needShutdown() {
        try {
            Integer oldApiVersion = BaseUtils.getApiVersion();
            Integer newApiVersion = remoteLogics.getApiVersion();
            return !oldApiVersion.equals(newApiVersion);
        } catch (RemoteException e) {
            return false;
        }
    }
    
    public class UserInfo {
        String name;
        boolean savePassword;
        String password;
        
        public UserInfo (String name, boolean savePassword, String password) {
            this.name = name;
            this.savePassword = savePassword;
            if (savePassword) {
                this.password = password;
            }
        }

        @Override
        public String toString() {
            String string = this.name + "\t" + savePassword;
            if (savePassword) {
                string += "\t" + Base64.encodeBase64URLSafeString(password.getBytes()) ;
            }
            return string;
        }
    }
}
