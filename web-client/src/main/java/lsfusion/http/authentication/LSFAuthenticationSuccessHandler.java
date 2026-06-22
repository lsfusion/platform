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

        // Prefer the `returnTo` request param (set by OAuthAuthorizeHandler and carried through
        // the login form into /login_check) over the session-backed saved request: the param
        // survives a login session rotation that can drop the session attribute on some servlet
        // containers, which would otherwise bounce the user to the app instead of OAuth consent.
        // Restricted to a local /oauth/authorize path to avoid an open redirect.
        String returnTo = request.getParameter("returnTo");
        if (isValidOAuthReturnTo(returnTo)) {
            getRedirectStrategy().sendRedirect(request, response, returnTo);
            return;
        }

        String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);
        if (savedRequest != null) {
            getRedirectStrategy().sendRedirect(request, response, savedRequest);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    // Only a local path to the OAuth authorize endpoint may be used as a post-login redirect
    // target — prevents /login?returnTo=//evil.example (or a scheme-bearing URL) from turning
    // login success into an open redirect. Shared with TwoFactorAuthenticationFilter so the 2FA
    // success path applies the same guard (package-visible).
    static boolean isValidOAuthReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isEmpty())
            return false;
        if (!returnTo.startsWith("/") || returnTo.startsWith("//"))
            return false;
        for (int i = 0; i < returnTo.length(); i++) {
            char c = returnTo.charAt(i);
            if (c < 0x20 || c == 0x7f) // reject control chars (response-splitting guard)
                return false;
        }
        int q = returnTo.indexOf('?');
        String path = q >= 0 ? returnTo.substring(0, q) : returnTo;
        return path.equals("/oauth/authorize");
    }
}