package platform.client;

public class StartupProperties {

    public static final String PLATFORM_CLIENT_HOSTNAME = "platform.client.hostname";

    public static final String PLATFORM_CLIENT_HOSTPORT = "platform.client.hostport";

    public static final String PLATFORM_CLIENT_DB = "platform.client.db";

    public static final String PLATFORM_CLIENT_LOGICSNAME = "platform.client.logicsname";

    public static final String PLATFORM_CLIENT_USER = "platform.client.user";

    public static final String PLATFORM_CLIENT_SAVEPWD = "platform.client.savepwd";

    public static final String PLATFORM_CLIENT_PASSWORD = "platform.client.password";

    public static final String PLATFORM_CLIENT_AUTOLOGIN = "platform.client.autologin";

    public static final String PLATFORM_CLIENT_CONNECTION_LOST_TIMEOUT = "platform.client.connection.lost.timeout";

    public static final String PLATFORM_CLIENT_CONNECTION_LOST_PING_DELAY = "platform.client.connection.lost.ping.delay";

    public static final String PLATFORM_CLIENT_LOG_RMI = "platform.client.log.rmi";

    public static final String PLATFORM_CLIENT_LOG_BASEDIR = "platform.client.log.basedir";

    public static final String PLATFORM_CLIENT_PINGTIME = "platform.client.pingTime";

    public static final String PLATFORM_CLIENT_LOGO = "platform.client.logo";

    public static final String PLATFORM_CLIENT_FORMS = "platform.client.forms";

    public static final String PLATFORM_CLIENT_FORMSET = "platform.client.formset";

    public static final String PLATFORM_CLIENT_BLOCKER_ACTIVATION_OFF = "platform.client.blocker.activation.off";

    public static final String PLATFORM_CLIENT_BLOCKER_AUTORECONNECT = "platform.client.blocker.autoreconnect";

    public static final String PLATFORM_CLIENT_ISDEBUG = "platform.client.isdebug";
    
    public static final String PLATFORM_CLIENT_DECIMAL_SEPARATOR = "platform.client.decimal.separator";

    public static final String PLATFORM_CLIENT_ASYNC_TIMEOUT = "platform.client.async.timeout";

    public static final boolean dotSeparator = ".".equals(System.getProperty(StartupProperties.PLATFORM_CLIENT_DECIMAL_SEPARATOR));
    
    public final static boolean autoReconnect = System.getProperty(StartupProperties.PLATFORM_CLIENT_BLOCKER_AUTORECONNECT) != null;

    public final static boolean preventBlockerActivation = autoReconnect || System.getProperty(StartupProperties.PLATFORM_CLIENT_BLOCKER_ACTIVATION_OFF) != null;
}
