package lsfusion.http.authentication;

import lsfusion.interop.connection.AuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Locale;

public class LSFAuthenticationToken extends UsernamePasswordAuthenticationToken {
    
    public final AuthenticationToken appServerToken;
    private final Locale locale; // optimization

    public static AuthenticationToken getAppServerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth instanceof LSFAuthenticationToken)
            return ((LSFAuthenticationToken) auth).appServerToken;
        // can be only for calls that are not under spring security: first lines with security="none" in applicationContext-security.xml
//        if (shouldBeAuthenticated) {
//            auth = new TestingAuthenticationToken("admin", "fusion");
//            throw new IllegalStateException(ServerMessages.getString(request, "error.user.must.be.authenticated"));
//        }
        return AuthenticationToken.ANONYMOUS;
    }

    public static Locale getUserLocale() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth instanceof LSFAuthenticationToken)
            return ((LSFAuthenticationToken) auth).locale;
        // can be only for calls that are not under spring security: first lines with security="none" in applicationContext-security.xml
//        if (shouldBeAuthenticated) {
////            auth = new TestingAuthenticationToken("admin", "fusion");
//            throw new IllegalStateException(ServerMessages.getString(request, "error.user.must.be.authenticated"));
//        }
        return null;
    }

    public static Locale getUserLocale(Authentication authentication) { // it is called after successfull authentication        
        return ((LSFAuthenticationToken) authentication).locale;
    }

    public LSFAuthenticationToken(Object principal, String credentials, AuthenticationToken appServerToken, Locale locale) {
        super(principal, credentials, new ArrayList<GrantedAuthority>());

        this.appServerToken = appServerToken;
        this.locale = locale;
    }
    
    public boolean isAnonymous() {
        return appServerToken == AuthenticationToken.ANONYMOUS;
    }
}
