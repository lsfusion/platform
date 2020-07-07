package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.file.FileData;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.base.exception.LockedException;
import lsfusion.interop.base.exception.LoginException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.connection.authentication.PasswordAuthentication;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import org.json.JSONObject;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Locale;

public class LSFRemoteAuthenticationProvider extends LogicsRequestHandler implements AuthenticationProvider {

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        if(authentication instanceof LSFAuthenticationToken) // already authenticated with LSFAuthTokenFilter
            return authentication;

        final String username = authentication.getPrincipal().toString();
        final String password = authentication.getCredentials().toString();

        try {
            //https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
            HttpServletRequest request = null;
            final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs != null)
                request = ((ServletRequestAttributes) attribs).getRequest();

            Pair<AuthenticationToken, Locale> authLocale = runRequest(request, new LogicsRunnable<Pair<AuthenticationToken, Locale>>() {
                public Pair<AuthenticationToken, Locale> run(LogicsSessionObject sessionObject) throws RemoteException {
                    try {
                        AuthenticationToken authToken = sessionObject.remoteLogics.authenticateUser(new PasswordAuthentication(username, password));
                        return new Pair<>(authToken, getUserLocale(sessionObject.remoteLogics, authentication, authToken));
                    } catch (LoginException le) {
                        throw new UsernameNotFoundException(le.getMessage());
                    } catch (LockedException le) {
                        throw new org.springframework.security.authentication.LockedException(le.getMessage());
                    } catch (RemoteMessageException le) {
                        throw new RuntimeException(le.getMessage());
                    }
                }
            });

            return new LSFAuthenticationToken(username, password, authLocale.first, authLocale.second);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    protected static Locale getUserLocale(RemoteLogicsInterface remoteLogics, Authentication auth, AuthenticationToken authToken) throws RemoteException {
        try {
            SessionInfo sessionInfo = NavigatorProviderImpl.getSessionInfo(auth);
            ExternalResponse result = remoteLogics.exec(authToken, sessionInfo, "Authentication.getCurrentUserLocale", new ExternalRequest());
            JSONObject localeObject = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes(), StandardCharsets.UTF_8));
            String language = localeObject.optString("language");
            String country = localeObject.optString("country");
            return LocalePreferences.getLocale(language, country);
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }

    public boolean supports(Class<? extends Object> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
