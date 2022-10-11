package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.authentication.OAuth2Authentication;
import lsfusion.interop.logics.LogicsSessionObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static lsfusion.http.authentication.LSFRemoteAuthenticationProvider.getUserLocale;

public class OAuth2ToLSFTokenFilter extends OncePerRequestFilter {
    public static final String AUTH_SECRET_KEY = "authSecret";

    @Autowired
    private LogicsProvider logicsProvider;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private LSFClientRegistrationRepository clientRegistrations;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        convertToken(logicsProvider, request, response, SecurityContextHolder.getContext().getAuthentication(), servletContext, clientRegistrations);
        filterChain.doFilter(request, response);
    }

    public static Authentication convertToken(LogicsProvider logicsProvider, HttpServletRequest request, HttpServletResponse response,
                                              Authentication authentication, ServletContext servletContext, LSFClientRegistrationRepository clientRegistrations) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken) || clientRegistrations == null) {
            return authentication;
        }
        String authorizedClientRegistrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String userNameAttributeName = clientRegistrations.
                findByRegistrationId(authorizedClientRegistrationId)
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String username = authorizedClientRegistrationId + ":" + principal.getAttribute(userNameAttributeName);
        LSFAuthenticationToken lsfAuthentication;
        String authSecret = servletContext.getInitParameter(AUTH_SECRET_KEY);
        try {
            Pair<AuthenticationToken, Locale> authLocale = logicsProvider.runRequest(request, (LogicsSessionObject sessionObject, boolean retry) -> {
                try {
                    Map<String, Object> userInfo = new HashMap<>(principal.getAttributes());
                    AuthenticationToken authToken = sessionObject.remoteLogics.authenticateUser(new OAuth2Authentication(username, authSecret, userInfo));
                    return new Pair<>(authToken, getUserLocale(sessionObject.remoteLogics, authentication, authToken, request));
                } catch (LockedException le) {
                    throw new org.springframework.security.authentication.LockedException(le.getMessage());
                }
            });

            lsfAuthentication = new LSFAuthenticationToken(username, null, authLocale.first, authLocale.second);
        } catch (org.springframework.security.authentication.LockedException | RemoteMessageException e) {
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
            response.sendRedirect(MainController.getURLPreservingParameters("/login", null, request));
            lsfAuthentication = null;
        } catch (AppServerNotAvailableDispatchException e) {
            throw Throwables.propagate(e);
        }
        SecurityContextHolder.getContext().setAuthentication(lsfAuthentication);
        return lsfAuthentication;
    }
}
