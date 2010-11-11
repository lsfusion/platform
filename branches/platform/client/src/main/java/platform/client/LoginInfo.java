package platform.client;

public class LoginInfo {
    private String serverHost;
    private String serverPort;
    private String userName;
    private String password;

    public LoginInfo(String serverHost, String serverPort, String userName, String password) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.userName = userName;
        this.password = password;
    }

    public String getServerHost() {
        return serverHost;
    }

    public String getServerPort() {
        return serverPort;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
