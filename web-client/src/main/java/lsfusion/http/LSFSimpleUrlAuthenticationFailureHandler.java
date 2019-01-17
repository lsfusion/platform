package lsfusion.http;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LSFSimpleUrlAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        saveException(request, exception);
        getRedirectStrategy().sendRedirect(request, response, "/login.jsp" + "?" + getQueryString(request));
    }

    private String getQueryString(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if(queryString == null)
            queryString = "";
        return queryString + (queryString.isEmpty() ? "" : "&") + "error=1";
    }
}