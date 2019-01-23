package lsfusion.http;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Locale;

public class LSFAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    public final Locale locale;

    public LSFAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, Locale locale) {
        super(principal, credentials, authorities);
        
        this.locale = locale;
    }
}
