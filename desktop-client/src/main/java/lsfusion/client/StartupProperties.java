package lsfusion.client;

public class StartupProperties {

    public static final String LSFUSION_CLIENT_HOSTNAME = "lsfusion.client.hostname";

    public static final String LSFUSION_CLIENT_HOSTPORT = "lsfusion.client.hostport";

    public static final String LSFUSION_CLIENT_EXPORTNAME = "lsfusion.client.exportname";

    public static final String LSFUSION_CLIENT_USER = "lsfusion.client.user";

    public static final String LSFUSION_CLIENT_SAVEPWD = "lsfusion.client.savepwd";

    public static final String LSFUSION_CLIENT_PASSWORD = "lsfusion.client.password";

    public static final String LSFUSION_CLIENT_AUTOLOGIN = "lsfusion.client.autologin";

    public static final String LSFUSION_CLIENT_CONNECTION_LOST_TIMEOUT = "lsfusion.client.connection.lost.timeout";

    public static final String LSFUSION_CLIENT_CONNECTION_LOST_PING_DELAY = "lsfusion.client.connection.lost.ping.delay";

    public static final String LSFUSION_CLIENT_LOG_RMI = "lsfusion.client.log.rmi";

    public static final String LSFUSION_CLIENT_LOG_BASEDIR = "lsfusion.client.log.basedir";

    public static final String LSFUSION_CLIENT_PING_TIME = "lsfusion.client.pingTime";

    public static final String LSFUSION_CLIENT_LOGO = "lsfusion.client.logo";

    public static final String LSFUSION_CLIENT_BLOCKER_ACTIVATION_OFF = "lsfusion.client.blocker.activation.off";

    public static final String LSFUSION_CLIENT_BLOCKER_AUTORECONNECT = "lsfusion.client.blocker.autoreconnect";

    public static final String LSFUSION_CLIENT_ISDEBUG = "lsfusion.client.isdebug";
    
    public static final String LSFUSION_CLIENT_DECIMAL_SEPARATOR = "lsfusion.client.decimal.separator";

    public static final String LSFUSION_CLIENT_ASYNC_TIMEOUT = "lsfusion.client.async.timeout";

    public static final int pullMessagesPeriod = Integer.parseInt(System.getProperty(LSFUSION_CLIENT_PING_TIME, "1000"));

    public static final int pingDelay = Integer.parseInt(System.getProperty(LSFUSION_CLIENT_CONNECTION_LOST_PING_DELAY, "3000"));

    public static final boolean dotSeparator = ".".equals(System.getProperty(StartupProperties.LSFUSION_CLIENT_DECIMAL_SEPARATOR));
    
    public final static boolean autoReconnect = System.getProperty(StartupProperties.LSFUSION_CLIENT_BLOCKER_AUTORECONNECT) != null;

    public final static boolean preventBlockerActivation = autoReconnect || System.getProperty(StartupProperties.LSFUSION_CLIENT_BLOCKER_ACTIVATION_OFF) != null;

    public final static int rmiTimeout = Integer.valueOf(System.getProperty(LSFUSION_CLIENT_CONNECTION_LOST_TIMEOUT, "7200000"));
}
