package lsfusion.gwt.base.server.spring;

import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.rcp.RemoteAuthenticationException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GwtRemoteAuthenticationManager implements RemoteAuthenticationManager {
    @Autowired
    private BusinessLogicsProvider businessLogicProvider;
    
    @Override
    public Collection<GrantedAuthority> attemptAuthentication(String username, String password) throws RemoteAuthenticationException {
        try {
            List<GrantedAuthority> result = new ArrayList<>();
            List<String> roles = businessLogicProvider.getLogics().authenticateUser(username, password);
            for (String role : roles) {
                result.add(new GrantedAuthorityImpl(role));
            }
            return result;
        } catch (LoginException le) {
            throw new UsernameNotFoundException(le.getMessage()); 
        } catch (RemoteServerException e) {
            throw new RuntimeException("Ошибка во время чтения данных о пользователе.", e);
        } catch (RemoteException e) {
            businessLogicProvider.invalidate();
            throw new RuntimeException("Ошибка во время чтения данных о пользователе.", e);
        }
    }
}
