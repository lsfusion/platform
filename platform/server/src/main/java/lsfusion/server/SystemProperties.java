package lsfusion.server;

public class SystemProperties {
    public static final String GC_INTERVAL = "sun.rmi.dgc.server.gcInterval";

    public static final String DGC_LEASE_VALUE = "java.rmi.dgc.leaseValue";

    public static final String MAIL_MIME_ENCODEFILENAME = "mail.mime.encodefilename";

    public static final boolean isDebug = "true".equals(System.getProperty("lsfusion.server.isdebug"));

    public static final String settingsPath = System.getProperty("lsfusion.server.settingsPath", "conf/settings.xml");

    public static final boolean doNotCalculateStats = "true".equals(System.getProperty("lsfusion.server.logics.donotcalculatestats"));

    public static final String userDir = System.getProperty("user.dir");

    public static void setGCIntervalIfNotDefined(String value) {
        if (System.getProperty(GC_INTERVAL) == null) {
            System.setProperty(GC_INTERVAL, value);
        }
    }

    public static void setDGCLeaseValue() {
        System.setProperty(DGC_LEASE_VALUE, "30000");
    }

    public static void enableMailEncodeFileName() {
        System.setProperty(MAIL_MIME_ENCODEFILENAME, "true");
    }
}
