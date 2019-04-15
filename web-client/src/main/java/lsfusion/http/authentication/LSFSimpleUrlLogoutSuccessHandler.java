package lsfusion.http.authentication;

import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LSFSimpleUrlLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        return LSFAuthenticationFailureHandler.getCachedRequest("/login", request, response);
    }
}