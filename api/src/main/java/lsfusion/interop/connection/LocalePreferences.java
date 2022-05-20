package lsfusion.interop.connection;

import java.io.Serializable;
import java.util.Locale;

public class LocalePreferences implements Serializable {
    public Locale locale;
    public String timeZone;
    public Integer twoDigitYearStart;
    public String dateFormat;
    public String timeFormat;

    public LocalePreferences(Locale locale, String timeZone, Integer twoDigitYearStart, String dateFormat, String timeFormat) {
        this.locale = locale;
        this.timeZone = timeZone;
        this.twoDigitYearStart = twoDigitYearStart;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
    }

    public static Locale getLocale(String language, String country) {
        if (language != null) {
            return new Locale(language, country == null ? "" : country);
        }
        return null; // default
    }
}
