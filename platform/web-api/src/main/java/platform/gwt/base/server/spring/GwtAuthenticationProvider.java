package platform.gwt.base.server.spring;


import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.core.userdetails.UserDetails;
import platform.interop.remote.UserInfo;

public class GwtAuthenticationProvider extends DaoAuthenticationProvider {

    public GwtAuthenticationProvider(){
        setSaltSource(new SaltSource() {
            @Override
            public Object getSalt(UserDetails user) {
                return UserInfo.salt;
            }
        });
    };
}
