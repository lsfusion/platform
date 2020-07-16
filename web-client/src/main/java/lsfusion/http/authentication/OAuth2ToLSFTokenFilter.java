package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.authentication.TrustedAuthentication;
import lsfusion.interop.logics.LogicsSessionObject;
import org.springframework.beans.factory.InitializingBean;
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
import java.util.Locale;

import static lsfusion.http.authentication.LSFRemoteAuthenticationProvider.getUserLocale;

public class OAuth2ToLSFTokenFilter extends OncePerRequestFilter implements InitializingBean {
    private static final String authSecretKey = "authSecret";

    @Autowired
    private LogicsProvider logicsProvider1;

    @Autowired
    private ServletContext servletContext1;

    @Autowired
    private LSFClientRegistrationRepository clientRegistrations1;

    private static ServletContext servletContext;
    private static LogicsProvider logicsProvider;
    private static LSFClientRegistrationRepository clientRegistrations;

    @Override
    public void afterPropertiesSet() throws ServletException {
        OAuth2ToLSFTokenFilter.servletContext = servletContext1;
        OAuth2ToLSFTokenFilter.logicsProvider = logicsProvider1;
        OAuth2ToLSFTokenFilter.clientRegistrations = clientRegistrations1;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        convertToken(request, response, SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);
    }

    public static Authentication convertToken(HttpServletRequest request, HttpServletResponse response,
                                              Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return authentication;
        }
        String userNameAttributeName = clientRegistrations.
                findByRegistrationId(((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId())
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String username = String.valueOf((Object) principal.getAttribute(userNameAttributeName));
        LSFAuthenticationToken lsfAuthentication;
        String authSecret = servletContext.getInitParameter(authSecretKey);
        try {
            Pair<AuthenticationToken, Locale> authLocale = logicsProvider.runRequest(request, (LogicsSessionObject sessionObject) -> {
                try {
                    AuthenticationToken authToken = sessionObject.remoteLogics.authenticateUser(new TrustedAuthentication(username, authSecret));
                    return new Pair<>(authToken, getUserLocale(sessionObject.remoteLogics, authentication, authToken));
                } catch (LockedException le) {
                    throw new org.springframework.security.authentication.LockedException(le.getMessage());
                } catch (RemoteMessageException e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
            lsfAuthentication = new LSFAuthenticationToken(username, null, authLocale.first, authLocale.second);
        } catch (org.springframework.security.authentication.LockedException e) {
            request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
            response.sendRedirect("/login");
            lsfAuthentication = null;
        } catch (AppServerNotAvailableDispatchException e) {
            throw Throwables.propagate(e);
        }
        SecurityContextHolder.getContext().setAuthentication(lsfAuthentication);
        return lsfAuthentication;
    }
}
