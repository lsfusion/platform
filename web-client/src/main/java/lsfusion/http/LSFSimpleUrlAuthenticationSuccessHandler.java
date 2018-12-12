package lsfusion.http;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LSFSimpleUrlAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String queryString = request.getQueryString();
        return "/lsfusion.jsp" + (queryString == null || queryString.isEmpty() ? "" : ("?" + queryString));
    }
}