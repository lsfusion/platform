package lsfusion.http;

import lsfusion.base.ServerMessages;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

import static lsfusion.base.ServerUtils.LOCALE_COOKIE_NAME;

// sets lsf user locale for all lsf endpoints
public class LSFLocaleFilter extends OncePerRequestFilter {
    public static final String LOCALE_PARAM_NAME = "locale";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        Locale newLocale = null;

        String newLocaleParam = request.getParameter(LOCALE_PARAM_NAME);
        if (newLocaleParam != null) {
            LocaleEditor localeEditor = new LocaleEditor();
            localeEditor.setAsText(newLocaleParam);
            newLocale = (Locale) localeEditor.getValue();
        }

        if(newLocale == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                newLocale = LSFAuthenticationToken.getLocale(auth);
            }
        }

        if(newLocale != null) {
            final Locale fNewLocale = newLocale;
            request = new HttpServletRequestWrapper(request) {
                public Locale getLocale() {
                    return fNewLocale;
                }
            };
        }
        
        // we need to apply RequestContextFilter for locale once again (we need it before authentication to use it in LSFRemoteAuthenticationProvider - to get browser locale)
        Locale prevLocale = null;
        if(newLocale != null) {
            prevLocale = LocaleContextHolder.getLocale();
            LocaleContextHolder.setLocale(newLocale);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if(newLocale != null)
                LocaleContextHolder.setLocale(prevLocale);
        }
    }
}
