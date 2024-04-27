package lsfusion.http.authentication;

import lsfusion.http.controller.MainController;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LSFAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    public LSFAuthenticationFailureHandler() {}

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        saveException(request, exception);

        getRedirectStrategy().sendRedirect(request, response, getCachedRequest("/login", request, response));
    }

    public static String getCachedRequest(String defaultURL, HttpServletRequest request, HttpServletResponse response) {
        String redirectUrl;
        String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);

        if (savedRequest == null) {
            redirectUrl = MainController.getURLPreservingParameters(defaultURL, null, request);
        } else {
            redirectUrl = savedRequest;
        }
        return redirectUrl;
    }
}