package lsfusion.http.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION;

public class LSFSimpleUrlLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        super.handle(request, response, authentication);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // to pass exceptions created in createNavigator() to login page after logout. "invalidate-session" in applicationContext-security is supposed to be set to "false". 
        HttpSession session = request.getSession();
        AuthenticationException authenticationException = (AuthenticationException) session.getAttribute(AUTHENTICATION_EXCEPTION);
        session.invalidate();
        
        if (authenticationException != null) {
            HttpSession newSession = request.getSession();
            newSession.setAttribute(AUTHENTICATION_EXCEPTION, authenticationException);
        }
        
        super.onLogoutSuccess(request, response, authentication);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        return LSFAuthenticationFailureHandler.getCachedRequest("/login", request, response);
    }
}