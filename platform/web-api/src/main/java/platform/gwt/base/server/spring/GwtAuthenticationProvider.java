package platform.gwt.base.server.spring;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import platform.interop.exceptions.LoginException;
import platform.interop.exceptions.RemoteServerException;
import platform.interop.remote.UserInfo;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class GwtAuthenticationProvider extends DaoAuthenticationProvider {

    public GwtAuthenticationProvider(){
        setSaltSource(new SaltSource() {
            @Override
            public Object getSalt(UserDetails user) {
                return UserInfo.salt;
            }
        });
    };

    private PasswordEncoder passwordEncoder = new Base64ShaPasswordEncoder(256);

    private UserDetailsService userDetailsService = new GwtLogicsUserDetailsService();

    @Override
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    @Override
    protected UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }
}
