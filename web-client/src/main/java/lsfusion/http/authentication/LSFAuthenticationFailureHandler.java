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
        StringBuilder paramString = new StringBuilder();
        String queryString = request.getQueryString();
        if (paramToRemove != null){
            List<String> params = Arrays.asList(queryString.split("&"));
            params = params.stream().filter(s -> !s.contains(paramToRemove)).collect(Collectors.toList());
            for (int i = 0; i < params.size(); i++) {
                if (i < params.size() - 1) {
                    paramString.append(params.get(i)).append("&");
                } else {
                    paramString.append(params.get(i));
                }
            }
            return url + "?" + paramString.toString();
        } else {
            queryString = !BaseUtils.isRedundantString(queryString) ? "?" + queryString : "";
            return url + queryString;
        }
    }
}