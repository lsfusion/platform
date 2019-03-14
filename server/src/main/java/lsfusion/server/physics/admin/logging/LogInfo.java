package lsfusion.server.physics.admin.logging;

public class LogInfo {
    public boolean allowExcessAllocatedBytes;
    public String userName;
    public String userRole;
    public String hostnameComputer;
    public String remoteAddress;
    
    public static LogInfo system = new LogInfo(true, "system", "system", "system", "system");

    public LogInfo(boolean allowExcessAllocatedBytes, String userName, String userRole, String hostnameComputer, String remoteAddress) {
        this.allowExcessAllocatedBytes = allowExcessAllocatedBytes;
        this.userName = userName;
        this.userRole = userRole;
        this.hostnameComputer = hostnameComputer;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String toString() {
        return "User : " + userName + ", Role: " + userRole + ", Allow Excess: " + allowExcessAllocatedBytes + ", Host : " + hostnameComputer + ", Remote : " + remoteAddress;
    }
}
