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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LSFRemoteAuthenticationManager extends LogicsRequestHandler implements RemoteAuthenticationManager {
    public HttpServletRequest request;

    public void setHttpServletRequest(HttpServletRequest request) {
       this.request = request;
    }

    @Override
    public Collection<GrantedAuthority> attemptAuthentication(final String username, final String password) throws RemoteAuthenticationException {

        try {
            return runRequest(request != null ? request.getParameter("host") : null, request != null ? parseInt(request.getParameter("port")) : null,
                    request != null ? request.getParameter("exportName") : null, new Runnable<Collection<GrantedAuthority>> () {
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
                        if (!oldApiVersion.equals(newApiVersion))
                            throw new DisabledException("Необходимо обновить web-клиент. изменилась версия API!");

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

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }
}
