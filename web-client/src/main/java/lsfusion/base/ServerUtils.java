package lsfusion.base;

import org.apache.commons.io.IOUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerUtils {
    public static final String LOCALE_COOKIE_NAME = "LSFUSION_LOCALE";
    // should be equal to ExternalHttpServer.HOSTNAME_COOKIE_NAME
    public static final String HOSTNAME_COOKIE_NAME = "LSFUSION_HOSTNAME";
    private static final String DEFAULT_LOCALE_LANGUAGE = "ru";

    public static Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Not authorized");
        }
        return auth;
    }

    public static String getAuthorizedUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "Not authorized" : auth.getName();
    }

    public static Locale getLocale(HttpServletRequest request) {
        if(request != null)
            return request.getLocale();
        return LocaleContextHolder.getLocale(); // just in case
    }

    private static final ConcurrentHashMap<String, String> versions = new ConcurrentHashMap<>();
    public static Map getVersionedResources(HttpServletRequest request, String... resources) throws IOException {
        Map<String, String> versionedResources = new LinkedHashMap<>();
        for (String resource : resources) {
            String version = versions.get(resource);
            if (version == null)
                version = versions.put(resource, SystemUtils.generateID(IOUtils.toByteArray(request.getServletContext().getResourceAsStream("/" + resource))));

            versionedResources.put(resource + "?version=" + version, resource.substring(resource.lastIndexOf(".") + 1));
        }
        return versionedResources;
    }
}
