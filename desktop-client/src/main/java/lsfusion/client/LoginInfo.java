package lsfusion.client;

public class LoginInfo {
    private String serverHost;
    private String serverPort;
    private String serverDB;
    private UserInfo userInfo;
    private boolean useAnonymousUI;

    public LoginInfo(String serverHost, String serverPort, String serverDB, UserInfo userInfo, boolean useAnonymousUI) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.serverDB = serverDB;
        this.userInfo = userInfo;
        this.useAnonymousUI = useAnonymousUI;
    }

    public String getServerHost() {
        return serverHost;
    }
    
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerDB() {
        return serverDB;
    }

    public void setServerDB(String serverDB) {
        this.serverDB = serverDB;
    }
    
    public UserInfo getUserInfo() {
        return userInfo;
    }
    
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getUserName() {
        return userInfo != null ? userInfo.name : null;
    }

    public String getPassword() {
        return userInfo != null ? userInfo.password : null;
    }

    public boolean getSavePwd() {
        return userInfo != null && userInfo.savePassword;
    }

    public boolean isUseAnonymousUI() {
        return useAnonymousUI;
    }
}
