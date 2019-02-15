package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.http.provider.navigator.LogicsAndNavigatorProviderImpl;
import lsfusion.interop.LocalePreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.remote.AuthenticationToken;
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
import java.rmi.RemoteException;
import java.util.Locale;

public class LSFRemoteAuthenticationProvider extends LogicsRequestHandler implements AuthenticationProvider {

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String username = authentication.getPrincipal().toString();
        final String password = authentication.getCredentials().toString();

        try {
            //https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
            HttpServletRequest request = null;
            final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs != null)
                request = ((ServletRequestAttributes) attribs).getRequest();

            Pair<AuthenticationToken, Locale> authLocale = runRequest(request, new Runnable<Pair<AuthenticationToken, Locale>> () {
                public Pair<AuthenticationToken, Locale> run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws RemoteException {
                    try {
                        AuthenticationToken authToken = remoteLogics.authenticateUser(username, password);
                        return new Pair<>(authToken, getUserLocale(remoteLogics, authentication, authToken));
                    } catch (LoginException le) {
                        throw new UsernameNotFoundException(le.getMessage());
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

    private static Locale getUserLocale(RemoteLogicsInterface remoteLogics, Authentication auth, AuthenticationToken authToken) throws RemoteException {
        SessionInfo sessionInfo = LogicsAndNavigatorProviderImpl.getSessionInfo(auth);
        ExecResult result = remoteLogics.exec(authToken, sessionInfo, "Authentication.getCurrentUserLocale", new String[0], new Object[0],
                "utf-8", new String[0], new String[0]);
        JSONObject localeObject = new JSONObject(new String(((FileData) result.results[0]).getRawFile().getBytes()));
        String language = localeObject.getString("language");
        String country = localeObject.getString("country");
        return LocalePreferences.getLocale(language, country);
    }

    public boolean supports(Class<? extends Object> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
