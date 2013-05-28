package platform.gwt.base.server.spring;

import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

public class RequestContextFilter extends OncePerRequestFilter {
    private static final String LOCALE_PARAM_NAME = "locale";

    private static final String LOCALE_COOKIE_NAME = "skolkovo.locale.current";

    private final CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();

    public RequestContextFilter() {
        cookieLocaleResolver.setCookieName(LOCALE_COOKIE_NAME);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        setNewLocaleIfNeeded(request, response);

        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        LocaleContextHolder.setLocale(cookieLocaleResolver.resolveLocale(request), true);
        RequestContextHolder.setRequestAttributes(attributes, true);
        if (logger.isDebugEnabled()) {
            logger.debug("Bound request context to thread: " + request);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
            RequestContextHolder.resetRequestAttributes();
            attributes.requestCompleted();
            if (logger.isDebugEnabled()) {
                logger.debug("Cleared thread-bound request context: " + request);
            }
        }
    }

    private void setNewLocaleIfNeeded(HttpServletRequest request, HttpServletResponse response) {
        String newLocale = request.getParameter(LOCALE_PARAM_NAME);
        if (newLocale != null) {
            LocaleEditor localeEditor = new LocaleEditor();
            localeEditor.setAsText(newLocale);
            cookieLocaleResolver.setLocale(request, response, (Locale) localeEditor.getValue());
        }
    }
}
