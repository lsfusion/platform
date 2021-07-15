package lsfusion.server.physics.admin;

public class SystemProperties {

    public static final boolean isPluginEnabled = System.getProperty("lsfusion.server.plugin.enabled") != null;
    public static final boolean isActionDebugEnabled = System.getProperty("lsfusion.server.debug.actions") != null;

    public static final boolean lightStart;
    public static final boolean inDevMode; 
    public static final boolean inTestMode;
    public static final String ideaBinPath = System.getProperty("idea.bin.path");
    
    static {
        String lightStartValue = System.getProperty("lsfusion.server.lightstart");
        String devModePropertyValue = System.getProperty("lsfusion.server.devmode");
        String testModePropertyValue = System.getProperty("lsfusion.server.testmode");

        lightStart = lightStartValue == null ? false : "true".equals(lightStartValue.toLowerCase());
        inDevMode  = devModePropertyValue == null ? isPluginEnabled : "true".equals(devModePropertyValue.toLowerCase());
        inTestMode = testModePropertyValue == null ? getAssertsStatus() : "true".equals(testModePropertyValue.toLowerCase());
    }

    // https://docs.oracle.com/javase/7/docs/technotes/guides/language/assert.html#design-faq-enable-disable 
    private static boolean getAssertsStatus() {
        boolean assertsEnabled = false;
        assert assertsEnabled = true;
        return assertsEnabled;
    }

    public static final boolean doNotCalculateStats = "true".equals(System.getProperty("lsfusion.server.logics.donotcalculatestats"));

    public static final String userDir = System.getProperty("user.dir");

    public static void setDGCParams() {
        if (System.getProperty("sun.rmi.dgc.server.leaseValue") == null) {
            System.setProperty("java.rmi.dgc.leaseValue", "1800000");
            System.setProperty("java.rmi.dgc.checkInterval", "900000");
            System.setProperty("sun.rmi.dgc.server.gcInterval", String.valueOf(Long.MAX_VALUE)); // отключаем по сути
        }
    }

    public static void enableMailEncodeFileName() {
        System.setProperty("mail.mime.encodefilename", "true");
    }
}
