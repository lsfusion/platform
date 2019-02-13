package lsfusion.http;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Locale;

public class LSFAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    public final Locale locale;
    public final String password; // temp

    public static Locale getLocale(Authentication authentication) {        
        if(authentication instanceof LSFAuthenticationToken)
            return ((LSFAuthenticationToken) authentication).locale;
        return null;
    }

    public static String getPassword(Authentication authentication) {
        if(authentication instanceof LSFAuthenticationToken)
            return ((LSFAuthenticationToken) authentication).password;
        return null;
    }

    public LSFAuthenticationToken(Object principal, String credentials, Collection<? extends GrantedAuthority> authorities, Locale locale) {
        super(principal, credentials, authorities);
        
        this.locale = locale;
        this.password = credentials;
    }
}
