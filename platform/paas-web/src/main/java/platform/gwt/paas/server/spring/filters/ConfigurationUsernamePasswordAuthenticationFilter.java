package platform.gwt.paas.server.spring.filters;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static platform.gwt.paas.server.spring.ConfigurationsUrlHelper.*;

public class ConfigurationUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public ConfigurationUsernamePasswordAuthenticationFilter() {
        setFilterProcessesUrl("/login_check");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!requiresAuthentication(request, response)) {
            chain.doFilter(request, response);
            return;
        }

        boolean isConfigurationLogout = isConfigurationRequest(request);
        Authentication authResult;
        try {
            authResult = attemptAuthentication(request, response);
            if (authResult == null) {
                // return immediately as subclass has indicated that it hasn't completed authentication
                return;
            }
        } catch (AuthenticationException exception) {
            // Authentication failed
            unsuccessfulAuthentication(request, response, isConfigurationLogout, exception);
            return;
        }

        successfulAuthentication(request, response, isConfigurationLogout, authResult);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String filterProcessesUrl = getFilterProcessesUrl();

        String uri = request.getRequestURI();
        int pathParamIndex = uri.indexOf(';');

        if (pathParamIndex > 0) {
            // strip everything after the first semi-colon
            uri = uri.substring(0, pathParamIndex);
        }

        return uri.endsWith(filterProcessesUrl);
    }

    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, boolean isConfigurationLogout, AuthenticationException exception) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        if (isConfigurationLogout) {
            saveException(request, exception);
            redirectStrategy.sendRedirect(request, response, getUnsuccessfullLoginUrl(request));
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed: " + exception.getMessage());
        }
    }

    protected final void saveException(HttpServletRequest request, AuthenticationException exception) {
        request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
    }

    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, boolean isConfigurationLogout, Authentication authResult) throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authResult);

        if (isConfigurationLogout) {
            redirectStrategy.sendRedirect(request, response, getSuccessfullLoginUrl(request));
        } else {
            response.setContentType("text/plain");
            response.getWriter().print(getSuccessfullLoginConstant());
        }
        clearAuthenticationAttributes(request);
    }

    /**
     * Removes temporary authentication-related data which may have been stored in the session
     * during the authentication process.
     */
    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        session.removeAttribute(WebAttributes.LAST_USERNAME);
    }
}
