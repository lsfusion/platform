package lsfusion.http;

import lsfusion.base.ServerUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

public class LSFAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
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
}