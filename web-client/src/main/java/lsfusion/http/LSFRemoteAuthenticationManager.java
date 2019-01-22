package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteMessageException;
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

import static lsfusion.base.ServerMessages.getString;

public class LSFRemoteAuthenticationManager extends LogicsRequestHandler implements RemoteAuthenticationManager {

    @Override
    public Collection<GrantedAuthority> attemptAuthentication(final String username, final String password) throws RemoteAuthenticationException {

        try {
            //https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
            String host = null;
            Integer port = null;
            String exportName = null;
            HttpServletRequest request = null;
            final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs != null) {
                request = ((ServletRequestAttributes) attribs).getRequest();
                host = request.getParameter("host");
                port = BaseUtils.parseInt(request.getParameter("port"));
                exportName = request.getParameter("exportName");
            }

            final HttpServletRequest finalRequest = request;
            return runRequest(host, port, exportName, new Runnable<Collection<GrantedAuthority>> () {
                public Collection<GrantedAuthority> run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws RemoteException {
                    try {
                        List<GrantedAuthority> result = new ArrayList<>();
                        List<String> roles = remoteLogics.authenticateUser(username, password);
                        for (String role : roles) {
                            result.add(new GrantedAuthorityImpl(role));
                        }

                        //TODO: добавить проверку platform version!
                        Integer oldApiVersion = BaseUtils.getApiVersion();
                        Integer newApiVersion = remoteLogics.getApiVersion();
                        if (!oldApiVersion.equals(newApiVersion)) {
                            throw new DisabledException(getString(finalRequest, "need.to.update.web.client"));
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
