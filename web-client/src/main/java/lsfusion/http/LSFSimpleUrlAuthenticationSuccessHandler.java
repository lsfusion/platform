package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.ServerUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.gwt.server.logics.provider.LogicsHandlerProviderImpl;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.interop.RemoteLogicsInterface;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LSFSimpleUrlAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {
        // setting cookie before super.onAuthenticationSuccess() to have right cookie-path  
        if(authentication instanceof LSFAuthenticationToken) {
            Locale userLocale = ((LSFAuthenticationToken) authentication).locale;
            if(userLocale != null) {
                Cookie localeCookie = new Cookie(ServerUtils.LOCALE_COOKIE_NAME, userLocale.toString());
                localeCookie.setMaxAge(60 * 60 * 24 * 365 * 5);
                response.addCookie(localeCookie);
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String queryString = request.getQueryString();
        if (queryString != null) {
            queryString = queryString.replaceAll("&error=1|error=1&|error=1", "");
        }
        return "/lsfusion.jsp" + (queryString == null || queryString.isEmpty() ? "" : ("?" + queryString));
    }
}