package lsfusion.http.authentication;

import lsfusion.base.ServerUtils;
import lsfusion.http.provider.logics.LogicsProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

public class LSFAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LogicsProvider logicsProvider;
    private final ServletContext servletContext;
    private final LSFClientRegistrationRepository clientRegistrations;

    public LSFAuthenticationSuccessHandler(LogicsProvider logicsProvider, ServletContext servletContext, LSFClientRegistrationRepository clientRegistrations) {
        this.logicsProvider = logicsProvider;
        this.servletContext = servletContext;
        this.clientRegistrations = clientRegistrations;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        authentication = OAuth2ToLSFTokenFilter.convertToken(logicsProvider, request, response, authentication, servletContext, clientRegistrations);
        if (authentication == null) {
            return;
        }
        // setting cookie before super.onAuthenticationSuccess() to have right cookie-path
        Cookie localeCookie = new Cookie(ServerUtils.LOCALE_COOKIE_NAME, "");
        Locale userLocale = LSFAuthenticationToken.getUserLocale(authentication);
        if (userLocale != null) {
            localeCookie.setValue(userLocale.toString());
            localeCookie.setMaxAge(60 * 60 * 24 * 365 * 5);
        } else {
            // removes cookie
            localeCookie.setMaxAge(0);
        }
        response.addCookie(localeCookie);

        String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);
        if (savedRequest != null) {
            getRedirectStrategy().sendRedirect(request, response, savedRequest);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}