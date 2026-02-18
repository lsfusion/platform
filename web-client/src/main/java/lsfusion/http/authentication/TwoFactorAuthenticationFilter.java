package lsfusion.http.authentication;

import lsfusion.base.ServerMessages;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Spring Security filter that handles the second authentication factor (2FA).
 *
 * <p>Sits in the filter chain after FORM_LOGIN_FILTER and intercepts POST /2fa.
 * The code is verified against the app-server (which generated and stored it);
 * the web-client never holds the plaintext code.
 */
public class TwoFactorAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final int MAX_ATTEMPTS = 5;

    private final LogicsProvider logicsProvider;

    public TwoFactorAuthenticationFilter(LogicsProvider logicsProvider) {
        super(new AntPathRequestMatcher("/2fa", "POST"));
        this.logicsProvider = logicsProvider;
        // No-op manager: we verify against app-server, not via Spring's AuthenticationManager.
        setAuthenticationManager(authentication -> authentication);
        setAuthenticationSuccessHandler(new TwoFactorSuccessHandler());
        setAuthenticationFailureHandler(new TwoFactorFailureHandler());
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new TwoFactorExpiredException();
        }

        // Cancel: user chose to go back to the login form.
        if ("true".equals(request.getParameter("cancel"))) {
            clearSession(session);
            throw new TwoFactorCancelledException();
        }

        Authentication storedAuth = (Authentication) session.getAttribute("2fa_user");
        if (storedAuth == null) {
            throw new TwoFactorExpiredException();
        }

        // Brute-force protection: track attempts in the session.
        Integer attempts = (Integer) session.getAttribute("2fa_attempts");
        attempts = (attempts == null ? 0 : attempts) + 1;
        session.setAttribute("2fa_attempts", attempts);
        if (attempts > MAX_ATTEMPTS) {
            clearSession(session);
            throw new TwoFactorLockedException();
        }

        // Delegate code verification to the app-server.
        String code = request.getParameter("code");
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", storedAuth.getName());
        jsonObject.put("code", code != null ? code : "");
        jsonArray.put(jsonObject);

        JSONObject jsonResponse = MainController.sendRequest(logicsProvider, jsonArray, request,
                "Authentication.verify2FACode");

        if (jsonResponse.has("success")) {
            clearSession(session);
            return storedAuth; // Spring Security will persist this into the SecurityContext.
        }

        String error = jsonResponse.optString("error", "invalid");
        if ("expired".equals(error)) {
            clearSession(session);
            throw new TwoFactorExpiredException();
        }
        // Wrong code — keep session so the user can retry.
        throw new BadCredentialsException("two.fa.error");
    }

    private void clearSession(HttpSession session) {
        session.removeAttribute("2fa_user");
        session.removeAttribute("2fa_attempts");
    }

    // -------------------------------------------------------------------------
    // Success handler: redirect to the originally requested page (or /main).
    // Does NOT re-check use2FA to avoid a redirect loop.
    // -------------------------------------------------------------------------
    private static class TwoFactorSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

        TwoFactorSuccessHandler() {
            super("/main");
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {
            String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);
            if (savedRequest != null) {
                getRedirectStrategy().sendRedirect(request, response, savedRequest);
            } else {
                super.onAuthenticationSuccess(request, response, authentication);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Failure handler: decides whether to keep the 2FA form or return to login.
    // -------------------------------------------------------------------------
    private static class TwoFactorFailureHandler implements AuthenticationFailureHandler {

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                            AuthenticationException exception) throws IOException {
            HttpSession session = request.getSession(false);

            if (exception instanceof TwoFactorCancelledException) {
                // No error message — just go back to the regular login form.

            } else if (exception instanceof BadCredentialsException
                    && session != null && session.getAttribute("2fa_user") != null) {
                // Wrong code but user still has attempts left — keep the 2FA form.
                session.setAttribute("2fa_error", true);

            } else if (session != null) {
                // Expired or locked — show the login form with a localized message.
                session.setAttribute("SPRING_SECURITY_LAST_EXCEPTION",
                        ServerMessages.getString(request, exception.getMessage()));
            }

            response.sendRedirect(MainController.getDirectUrl("/login", request));
        }
    }

    // -------------------------------------------------------------------------
    // Custom exception types (message = i18n key used by the failure handler).
    // -------------------------------------------------------------------------

    public static class TwoFactorExpiredException extends AuthenticationException {
        public TwoFactorExpiredException() { super("two.fa.expired"); }
    }

    public static class TwoFactorLockedException extends AuthenticationException {
        public TwoFactorLockedException() { super("two.fa.attempts"); }
    }

    public static class TwoFactorCancelledException extends AuthenticationException {
        public TwoFactorCancelledException() { super("cancelled"); }
    }
}
