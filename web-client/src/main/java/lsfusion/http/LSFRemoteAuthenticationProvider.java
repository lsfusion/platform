package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.remote.PreAuthentication;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class LSFRemoteAuthenticationProvider extends LogicsRequestHandler implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final String username = authentication.getPrincipal().toString();
        final String password = authentication.getCredentials().toString();

        try {
            //https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
            HttpServletRequest request = null;
            final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs != null)
                request = ((ServletRequestAttributes) attribs).getRequest();

            PreAuthentication auth = runRequest(request, new Runnable<PreAuthentication> () {
                public PreAuthentication run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws RemoteException {
                    try {
                        Locale locale = LocaleContextHolder.getLocale();
                        return remoteLogics.preAuthenticateUser(username, password, locale.getLanguage(), locale.getCountry());
                    } catch (LoginException le) {
                        throw new UsernameNotFoundException(le.getMessage());
                    } catch (RemoteMessageException le) {
                        throw new RuntimeException(le.getMessage());
                    }
                }
            });

            Collection<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : auth.roles) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
            return new LSFAuthenticationToken(username, password, authorities, auth.locale);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean supports(Class<? extends Object> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
