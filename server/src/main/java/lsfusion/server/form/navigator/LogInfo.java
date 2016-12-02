package lsfusion.server.form.navigator;

public class LogInfo {
    public boolean allowExcessAllocatedBytes;
    public String userName;
    public String hostnameComputer;
    public String remoteAddress;
    
    public static LogInfo system = new LogInfo(true, "system", "system", "system");

    public LogInfo(boolean allowExcessAllocatedBytes, String userName, String hostnameComputer, String remoteAddress) {
        this.allowExcessAllocatedBytes = allowExcessAllocatedBytes;
        this.userName = userName;
        this.hostnameComputer = hostnameComputer;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String toString() {
        return "User : " + userName + ", Allow Excess: " + allowExcessAllocatedBytes + ", Host : " + hostnameComputer + ", Remote : " + remoteAddress;
    }
}
