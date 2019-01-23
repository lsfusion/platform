package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteMessageException;
import lsfusion.interop.remote.PreAuthentication;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationManager;
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

public class LSFRemoteAuthenticationManager extends LogicsRequestHandler implements RemoteAuthenticationManager {

    @Override
    public Collection<GrantedAuthority> attemptAuthentication(final String username, final String password) throws RemoteAuthenticationException {

        try {
            //https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
            HttpServletRequest request = null;
            final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs != null)
                request = ((ServletRequestAttributes) attribs).getRequest();

            return runRequest(request, new Runnable<Collection<GrantedAuthority>> () {
                public Collection<GrantedAuthority> run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws RemoteException {
                    try {
                        List<GrantedAuthority> result = new ArrayList<>();
                        Locale locale = Locale.getDefault();
                        PreAuthentication auth = remoteLogics.preAuthenticateUser(username, password, locale.getLanguage(), locale.getCountry());
                        for (String role : auth.roles) {
                            result.add(new GrantedAuthorityImpl(role));
                        }

                        return result;
                    } catch (LoginException le) {
                        throw new UsernameNotFoundException(le.getMessage());
                    } catch (RemoteMessageException le) {
                        throw new RuntimeException(le.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

//    } catch (LoginException le) {
//        throw new UsernameNotFoundException(le.getMessage());
//    } catch (RemoteMessageException le) {
//        throw new RuntimeException(le.getMessage());
    }
}
