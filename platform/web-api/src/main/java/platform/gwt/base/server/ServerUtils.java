package platform.gwt.base.server;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;
import java.util.TimeZone;

public class ServerUtils {
    private static final String DEFAULT_LOCALE_LANGUAGE = "ru";
    public static ThreadLocal<TimeZone> timeZone = new ThreadLocal<TimeZone>();

    public static Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Not authorized");
        }
        return auth;
    }

    public static Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }

    public static String getLocaleLanguage() {
        String language = getLocale().getLanguage();
        if (!"ru".equals(language) && !"en".equals(language)) {
            return DEFAULT_LOCALE_LANGUAGE;
        }
        return language;
    }
}
