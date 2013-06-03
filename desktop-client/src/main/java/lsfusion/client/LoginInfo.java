package lsfusion.client;

public class LoginInfo {
    private String serverHost;
    private String serverPort;
    private String serverDB;
    private String userName;
    private String password;

    public LoginInfo(String serverHost, String serverPort, String serverDB, String userName, String password) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.serverDB = serverDB;
        this.userName = userName;
        this.password = password;
    }

    public String getServerHost() {
        return serverHost;
    }

    public String getServerPort() {
        return serverPort;
    }

    public String getServerDB() {
        return serverDB;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
