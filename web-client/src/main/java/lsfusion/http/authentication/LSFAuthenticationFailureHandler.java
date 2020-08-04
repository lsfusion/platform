package lsfusion.http.authentication;

import lsfusion.base.BaseUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LSFAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        saveException(request, exception);

        getRedirectStrategy().sendRedirect(request, response, getCachedRequest("/login", request, response));
    }

    public static String getCachedRequest(String defaultURL, HttpServletRequest request, HttpServletResponse response) {
        String redirectUrl;
        String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);

        if (savedRequest == null) {
            redirectUrl = getURLPreservingParameters(defaultURL, null, request);
        } else {
            redirectUrl = savedRequest;
        }
        return redirectUrl;
    }

    public static String getURLPreservingParameters(String url, String paramToRemove, HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (paramToRemove != null){
            List<String> params = Arrays.asList(queryString.split("&"));
            String paramString = params.stream().filter(s -> !s.contains(paramToRemove)).collect(Collectors.joining("&"));
            return paramString.length() > 0 ? url + "?" + paramString : url;
        } else {
            queryString = !BaseUtils.isRedundantString(queryString) ? "?" + queryString : "";
            return url + queryString;
        }
    }

    public static String getDirectUrl(String url, String query, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String queryString = request.getQueryString();
        if (query == null) {
            return queryString == null ? contextPath + url : contextPath + url + "?" + queryString;
        } else {
            return queryString == null ? contextPath + url + "?" + query : contextPath + url + "?" + queryString + "&" + query;
        }
    }

    public static String getRedirectUrl(String url, String paramToRemove, HttpServletRequest request) {
        return "redirect:" + getURLPreservingParameters(url, paramToRemove, request);
    }
}