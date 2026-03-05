package lsfusion.http.authentication;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.base.exception.LoginException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.connection.authentication.PasswordAuthentication;
import lsfusion.interop.logics.LogicsSessionObject;
import org.json.JSONObject;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LSFRemoteAuthenticationProvider extends LogicsRequestHandler implements AuthenticationProvider {
    public LSFRemoteAuthenticationProvider(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    protected static Locale getUserLocale(LogicsSessionObject sessionObject, Authentication auth, AuthenticationToken authToken, HttpServletRequest request) throws RemoteException {
        try {
            JSONObject localeObject = new JSONObject(MainController.sendRequest(authToken, request, ListFact.EMPTY(), sessionObject, NavigatorProviderImpl.getConnectionInfo(auth), "Authentication.getCurrentUserLocale"));
            String language = localeObject.optString("language");
            String country = localeObject.optString("country");
            return LocalePreferences.getLocale(language, country);
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        if(authentication instanceof LSFAuthenticationToken) // already authenticated with LSFAuthTokenFilter
            return authentication;

        final String username = authentication.getPrincipal().toString();
        final String password = authentication.getCredentials().toString();
        HttpServletRequest request = getHttpServletRequest();
        try {
            Pair<AuthenticationToken, Locale> authLocale = runRequest(request, (sessionObject, retry) -> {
                try {
                    AuthenticationToken authToken = sessionObject.remoteLogics.authenticateUser(new PasswordAuthentication(username, password));
                    return new Pair<>(authToken, getUserLocale(sessionObject, authentication, authToken, request));
                } catch (LoginException le) {
                    throw new UsernameNotFoundException(le.getMessage());
                } catch (LockedException le) {
                    throw new org.springframework.security.authentication.LockedException(le.getMessage());
                } catch (RemoteMessageException le) {
                    throw new RuntimeException(le.getMessage());
                }
            });

            return new LSFAuthenticationToken(username, password, authLocale.first, authLocale.second);
        } catch (Throwable e) {
            Map<String, String> userData = new HashMap<>();
            userData.put("username", username);
            request.getSession(true).setAttribute("USER_DATA", userData);
            throw new InternalAuthenticationServiceException(e.getMessage()); //need to throw AuthenticationException for SpringSecurity to redirect to /login
        }
    }

    public boolean supports(Class<? extends Object> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    //https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
    public static HttpServletRequest getHttpServletRequest() {
        HttpServletRequest request = null;
        final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs != null) {
            request = ((ServletRequestAttributes) attribs).getRequest();
        }
        return request;
    }
}
