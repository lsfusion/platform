package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.remote.PreAuthentication;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.rcp.RemoteAuthenticationException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static lsfusion.base.ServerMessages.getString;

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

            final HttpServletRequest finalRequest = request;
            PreAuthentication auth = runRequest(request, new Runnable<PreAuthentication> () {
                public PreAuthentication run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws RemoteException {
                    try {
                        Locale locale = Locale.getDefault();
                        PreAuthentication auth = remoteLogics.preAuthenticateUser(username, password, locale.getLanguage(), locale.getCountry());
                        //TODO: добавить проверку platform version!
                        Integer oldApiVersion = BaseUtils.getApiVersion();
                        Integer newApiVersion = remoteLogics.getApiVersion();
                        if (!oldApiVersion.equals(newApiVersion)) {
                            throw new DisabledException(getString(finalRequest, "need.to.update.web.client"));
                        }
                        return auth;
                    } catch (LoginException le) {
                        throw new UsernameNotFoundException(le.getMessage());
                    } catch (RemoteMessageException le) {
                        throw new RuntimeException(le.getMessage());
                    }
                }
            });

            Collection<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : auth.roles) {
                authorities.add(new GrantedAuthorityImpl(role));
            }
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password, authorities);
            token.setDetails(auth.locale);            
            return token;

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean supports(Class<? extends Object> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
