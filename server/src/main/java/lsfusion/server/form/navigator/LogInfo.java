package lsfusion.server.form.navigator;

public class LogInfo {
    public String userName;
    public String hostnameComputer;
    public String remoteAddress;
    
    public static LogInfo system = new LogInfo("system", "system", "system");

    public LogInfo(String userName, String hostnameComputer, String remoteAddress) {
        this.userName = userName;
        this.hostnameComputer = hostnameComputer;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String toString() {
        return "User : " + userName + ", Host : " + hostnameComputer + ", Remote : " + remoteAddress;
    }
}
