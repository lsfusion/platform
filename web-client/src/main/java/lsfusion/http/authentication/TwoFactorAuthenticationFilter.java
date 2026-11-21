package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.base.ServerMessages;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.connection.AuthenticationToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;

/**
 * Spring Security filter that handles the second authentication factor (2FA).
 *
 * <p>Sits in the filter chain after FORM_LOGIN_FILTER and intercepts POST /2fa: the login page's password step left the
 * app-server's token of the password in the session (LSFAuthenticationFailureHandler), the token is good only for the
 * check of the code. The code is verified against the app-server (which generated and stored it, and counts the failed
 * attempts); the web-client never holds the plaintext code.
 */
public class TwoFactorAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String USER = "2fa_user";

    private final LogicsProvider logicsProvider;

    public TwoFactorAuthenticationFilter(LogicsProvider logicsProvider, AuthenticationSuccessHandler successHandler) {
        super(new AntPathRequestMatcher("/2fa", "POST"));
        this.logicsProvider = logicsProvider;
        // No-op manager: we verify against app-server, not via Spring's AuthenticationManager.
        setAuthenticationManager(authentication -> authentication);
        // the user is authenticated here, not at the password step: here the session gets a new id
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
        setAuthenticationSuccessHandler(successHandler);
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

        LSFAuthenticationToken storedAuth = (LSFAuthenticationToken) session.getAttribute(USER);
        if (storedAuth == null) {
            throw new TwoFactorExpiredException();
        }

        // Delegate code verification to the app-server.
        String code = request.getParameter("code");
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("token", storedAuth.appServerToken.string);
        jsonObject.put("code", code != null ? code : "");
        jsonArray.put(jsonObject);

        JSONObject jsonResponse = MainController.sendRequest(logicsProvider, jsonArray, request,
                "Authentication.verify2FACode");

        if (jsonResponse.has("success")) {
            clearSession(session);
            AuthenticationToken token = new AuthenticationToken(jsonResponse.getString("token"));
            Locale locale;
            try {
                locale = logicsProvider.runRequest(request, (sessionObject, retry) -> LSFRemoteAuthenticationProvider.getUserLocale(sessionObject, storedAuth, token, request));
            } catch (AppServerNotAvailableDispatchException e) {
                throw Throwables.propagate(e);
            }
            return new LSFAuthenticationToken(storedAuth.getName(), null, token, locale); // Spring Security will persist this into the SecurityContext.
        }

        String error = jsonResponse.getString("error");
        if ("invalid".equals(error)) {
            // Wrong code — keep session so the user can retry.
            throw new BadCredentialsException("two.fa.error");
        }
        clearSession(session);
        throw "attempts".equals(error) ? new TwoFactorLockedException() : new TwoFactorExpiredException();
    }

    private void clearSession(HttpSession session) {
        session.removeAttribute(USER);
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
                    && session != null && session.getAttribute(USER) != null) {
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

    // the password of a user with the second factor is right: only the login page goes on, to the code
    // (LSFAuthenticationFailureHandler), any other login by the password is refused
    public static class TwoFactorRequiredException extends AuthenticationException {
        public final LSFAuthenticationToken authentication; // with the app-server's token of the password

        public TwoFactorRequiredException(LSFAuthenticationToken authentication) {
            super("The second factor is required");
            this.authentication = authentication;
        }
    }
}
