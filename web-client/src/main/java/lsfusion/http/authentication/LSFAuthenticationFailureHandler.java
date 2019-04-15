package lsfusion.http.authentication;

import lsfusion.base.BaseUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LSFAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        saveException(request, exception);

        getRedirectStrategy().sendRedirect(request, response, getCachedRequest("/login", request, response));
    }

    public static String getCachedRequest(String defaultURL, HttpServletRequest request, HttpServletResponse response) {
        String redirectUrl;
        SavedRequest savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request, response);

        if (savedRequest == null) {
            redirectUrl = getURLPreservingParameters(defaultURL, request);
        } else {
            redirectUrl = savedRequest.getRedirectUrl();
        }
        return redirectUrl;
    }

    public static String getURLPreservingParameters(String url, HttpServletRequest request) {
        String queryString = request.getQueryString();
        queryString = !BaseUtils.isRedundantString(queryString) ? "?" + queryString : "";
        return url + queryString;
    }
}