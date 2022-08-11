package lsfusion.base;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public static String getVersionedResources(HttpServletRequest request, String... resources) throws IOException {
        List<String> versionedResources = new ArrayList<>();
        for (String resource : resources) {
            String versionedResource = resource + "?version=" + SystemUtils.generateID(IOUtils.toByteArray(request.getServletContext().getResourceAsStream("/" + resource)));
            versionedResources.add(resource.endsWith(".js") ? "<script type='text/javascript' src='" + versionedResource + "'></script>"
                    : "<link rel='stylesheet' type='text/css' href='" + versionedResource + "' />");
        }
        return new Gson().toJson(versionedResources);
    }
}
