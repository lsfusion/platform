package lsfusion.gwt.server.base.spring;

import lsfusion.http.LSFRemoteAuthenticationManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

//https://stackoverflow.com/questions/24025924/java-lang-illegalstateexception-no-thread-bound-request-found-exception-in-asp
public class LSFRemoteAuthenticationProvider implements AuthenticationProvider, InitializingBean {
    private LSFRemoteAuthenticationManager remoteAuthenticationManager;

    public LSFRemoteAuthenticationProvider() {
    }

    public void afterPropertiesSet() {
        Assert.notNull(this.remoteAuthenticationManager, "remoteAuthenticationManager is mandatory");
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();

        RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs != null) {
            this.remoteAuthenticationManager.setHttpServletRequest(((ServletRequestAttributes) attribs).getRequest());
        }

        Collection<GrantedAuthority> authorities = this.remoteAuthenticationManager.attemptAuthentication(username, password);
        return new UsernamePasswordAuthenticationToken(username, password, authorities);
    }

    public LSFRemoteAuthenticationManager getRemoteAuthenticationManager() {
        return this.remoteAuthenticationManager;
    }

    public void setRemoteAuthenticationManager(LSFRemoteAuthenticationManager remoteAuthenticationManager) {
        this.remoteAuthenticationManager = remoteAuthenticationManager;
    }

    public boolean supports(Class<? extends Object> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}