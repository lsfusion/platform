package lsfusion.server.physics.admin.log;

public class LogInfo {
    public Boolean allowExcessAllocatedBytes;
    public String userName;
    public String userRoles;
    public String hostnameComputer;
    public String remoteAddress;
    
    public static LogInfo system = new LogInfo(true, "system", "system", "system", "system");

    public LogInfo(Boolean allowExcessAllocatedBytes, String userName, String userRoles, String hostnameComputer, String remoteAddress) {
        this.allowExcessAllocatedBytes = allowExcessAllocatedBytes;
        this.userName = userName;
        this.userRoles = userRoles;
        this.hostnameComputer = hostnameComputer;
        this.remoteAddress = remoteAddress;
    }

    public LogInfo(String userName, String userRoles, String hostnameComputer) {
        this(null, userName, userRoles, hostnameComputer, null);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (userName != null)
            stringBuilder.append("User : ").append(userName);
        if (userRoles != null)
            stringBuilder.append(", Role: ").append(userRoles);
        if (allowExcessAllocatedBytes != null)
            stringBuilder.append(", Allow Excess: ").append(allowExcessAllocatedBytes);
        if (hostnameComputer != null)
            stringBuilder.append(", Host : ").append(hostnameComputer);
        if (remoteAddress != null)
            stringBuilder.append(", Remote : ").append(remoteAddress);

        return stringBuilder.toString();
    }
}
