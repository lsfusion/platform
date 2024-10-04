package lsfusion.interop.connection;

import java.io.Serializable;
import java.util.TimeZone;

public class UserInfo implements Serializable {

    public static final UserInfo NULL = new UserInfo(null, null, null, null, null, null);
    public final String language;
    public final String country;
    public final TimeZone timeZone;
    public final String dateFormat;
    public final String timeFormat;
    public final String clientColorTheme;

    public UserInfo(String language, String country, TimeZone timeZone, String dateFormat, String timeFormat, String clientColorTheme) {
        this.language = language;
        this.country = country;
        this.timeZone = timeZone;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.clientColorTheme = clientColorTheme;
    }
}
