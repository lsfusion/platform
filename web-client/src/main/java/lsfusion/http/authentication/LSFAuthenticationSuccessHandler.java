package lsfusion.http.authentication;

import lsfusion.base.ServerUtils;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;

public class LSFAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LogicsProvider logicsProvider;
    private final ServletContext servletContext;
    private final LSFClientRegistrationRepository clientRegistrations;

    public LSFAuthenticationSuccessHandler(LogicsProvider logicsProvider, ServletContext servletContext, LSFClientRegistrationRepository clientRegistrations) {
        this.logicsProvider = logicsProvider;
        this.servletContext = servletContext;
        this.clientRegistrations = clientRegistrations;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        authentication = OAuth2ToLSFTokenFilter.convertToken(logicsProvider, request, response, authentication, servletContext, clientRegistrations);
        if (authentication == null) {
            return;
        }
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

        if (authentication instanceof LSFAuthenticationToken) {
            LSFAuthenticationToken lsfAuthentication = (LSFAuthenticationToken) authentication;
            if (lsfAuthentication.use2FA()) {
                SecurityContextHolder.clearContext();
                String username = authentication.getName();

                // Ask the app-server to generate, store, and send the 2FA code.
                // The web-client never sees the plaintext code.
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                jsonArray.put(jsonObject);

                HttpSession session = request.getSession();
                JSONObject jsonResponse = MainController.sendRequest(logicsProvider, jsonArray, request,
                        "Authentication.send2FACode");

                if (jsonResponse.has("error")) {
                    session.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.getString("error"));
                    response.sendRedirect(MainController.getDirectUrl("/login", request));
                    return;
                }

                // Store only the authenticated principal; the code lives on the app-server.
                session.setAttribute("2fa_user", authentication);
                response.sendRedirect(MainController.getDirectUrl("/login", request));
                return;
            }
        }

        String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);
        if (savedRequest != null) {
            getRedirectStrategy().sendRedirect(request, response, savedRequest);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}