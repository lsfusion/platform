package lsfusion.http.authentication;

import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LSFAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LogicsProvider logicsProvider;

    public LSFAuthenticationFailureHandler(LogicsProvider logicsProvider) {
        this.logicsProvider = logicsProvider;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        if (exception instanceof TwoFactorAuthenticationFilter.TwoFactorRequiredException) {
            LSFAuthenticationToken authentication = ((TwoFactorAuthenticationFilter.TwoFactorRequiredException) exception).authentication;

            // Ask the app-server to generate, store, and send the 2FA code.
            // The web-client never sees the plaintext code.
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("token", authentication.appServerToken.string);
            jsonArray.put(jsonObject);

            HttpSession session = request.getSession();
            JSONObject jsonResponse = MainController.sendRequest(logicsProvider, jsonArray, request,
                    "Authentication.send2FACode");

            if (jsonResponse.has("error")) {
                session.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", jsonResponse.getString("error"));
            } else {
                // the login page asks for the code then (TwoFactorAuthenticationFilter); the code lives on the app-server
                session.setAttribute(TwoFactorAuthenticationFilter.USER, authentication);
            }
            response.sendRedirect(MainController.getDirectUrl("/login", request));
            return;
        }

        saveException(request, exception);

        getRedirectStrategy().sendRedirect(request, response, getCachedRequest("/login", request));
    }

    public static String getCachedRequest(String defaultURL, HttpServletRequest request) {
        String redirectUrl;
        String savedRequest = LSFLoginUrlAuthenticationEntryPoint.requestCache.getRequest(request);

        if (savedRequest == null) {
            redirectUrl = MainController.getURLPreservingParameters(defaultURL, null, request);
        } else {
            redirectUrl = savedRequest;
        }
        return redirectUrl;
    }
}