package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.ServerUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class LSFSimpleUrlAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {
        // setting cookie before super.onAuthenticationSuccess() to have right cookie-path  
        Cookie localeCookie = new Cookie(ServerUtils.LOCALE_COOKIE_NAME, "");
        Locale userLocale = LSFAuthenticationToken.getUserLocale(authentication);
        if (userLocale != null) {
            localeCookie.setValue(userLocale.toString());
            localeCookie.setMaxAge(60 * 60 * 24 * 365 * 5);
        } else {
            // removes cookie
            localeCookie.setMaxAge(0);
        }
        response.addCookie(localeCookie);
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
    
    private String removeQueryParams(HttpServletRequest request, Set<String> paramsToRemove) {
        String queryString = request.getQueryString();
        StringBuilder newQueryString = new StringBuilder();
        try {
            if (queryString != null && !queryString.isEmpty()) {
                List<NameValuePair> params = new URIBuilder("?" + queryString).getQueryParams();
                for (Iterator<NameValuePair> paramsIterator = params.iterator(); paramsIterator.hasNext();) {
                    NameValuePair queryParameter = paramsIterator.next();
                    if (paramsToRemove.contains(queryParameter.getName())) {
                        paramsIterator.remove();
                    }
                }
                for (NameValuePair param : params) {
                    newQueryString.append(newQueryString.length() != 0 ? "&" : "?");
                    newQueryString.append(param);
                }
            }
        } catch (URISyntaxException e) {
            Throwables.propagate(e);
        } 
        return newQueryString.toString();
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String newQueryString = removeQueryParams(request, Collections.singleton("error"));
        return "/main" + newQueryString;
    }
}