package lsfusion.server.physics.admin.log;

public class LogInfo {
    public boolean allowExcessAllocatedBytes;
    public String userName;
    public String userRoles;
    public String hostnameComputer;
    public String remoteAddress;
    
    public static LogInfo system = new LogInfo(true, "system", "system", "system", "system");

    public LogInfo(boolean allowExcessAllocatedBytes, String userName, String userRoles, String hostnameComputer, String remoteAddress) {
        this.allowExcessAllocatedBytes = allowExcessAllocatedBytes;
        this.userName = userName;
        this.userRoles = userRoles;
        this.hostnameComputer = hostnameComputer;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public String toString() {
        return "User : " + userName + ", Role: " + userRoles + ", Allow Excess: " + allowExcessAllocatedBytes + ", Host : " + hostnameComputer + ", Remote : " + remoteAddress;
    }
}
