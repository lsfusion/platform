package lsfusion.http;

import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LSFSimpleUrlLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String queryString = request.getQueryString();
        return  "/login" + (queryString == null || queryString.isEmpty() ? "" : ("?" + queryString));
    }
}