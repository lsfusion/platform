package lsfusion.http;

import lsfusion.base.BaseUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LSFAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    // to save latest URL as in SavedRequestAwareAuthenticationSuccessHandler. Somehow is set in ExceptionTranslationFilter.
    private RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        saveException(request, exception);

        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (savedRequest == null) {
            String queryString = request.getQueryString();
            queryString = !BaseUtils.isRedundantString(queryString) ? "?" + queryString : "";
            getRedirectStrategy().sendRedirect(request, response, "/login" + queryString);
        } else {
            String targetUrl = savedRequest.getRedirectUrl();
            logger.debug("Redirecting to DefaultSavedRequest Url: " + targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}