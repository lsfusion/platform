package lsfusion.server;

public class SystemProperties {
    private static final int DEFAULT_DEBUGGER_PORT = 1299;
    
    public static final boolean isActionDebugEnabled = System.getProperty("lsfusion.server.debug.actions") != null;

    public static final boolean isDebug = "true".equals(System.getProperty("lsfusion.server.isdebug"));

    public static final String settingsPath = System.getProperty("lsfusion.server.settingsPath", "lsfusion.xml");

    public static final boolean doNotCalculateStats = "true".equals(System.getProperty("lsfusion.server.logics.donotcalculatestats"));

    public static final String userDir = System.getProperty("user.dir");

    public static void setGCIntervalIfNotDefined(String value) {
        if (System.getProperty("sun.rmi.dgc.server.gcInterval") == null) {
            System.setProperty("sun.rmi.dgc.server.gcInterval", value);
        }
    }

    public static void setDGCLeaseValue() {
        System.setProperty("java.rmi.dgc.leaseValue", "30000");
        System.setProperty("java.rmi.dgc.checkInterval", "15000");
    }

    public static void enableMailEncodeFileName() {
        System.setProperty("mail.mime.encodefilename", "true");
    }
    
    public static int getDebuggerPort() {
        String stringPort = System.getProperty("lsfusion.debugger.port");
        try {
            Integer port = Integer.valueOf(stringPort);
            if (port != null) {
                return port;
            }
        } catch (NumberFormatException ignored) {}
        return DEFAULT_DEBUGGER_PORT;
    }
}
