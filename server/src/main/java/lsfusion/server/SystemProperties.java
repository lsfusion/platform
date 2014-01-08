package lsfusion.server;

public class SystemProperties {

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
}
