package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.authentication.OAuth2Credentials;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LSFClientRegistrationRepository extends LogicsRequestHandler implements ClientRegistrationRepository, Iterable<ClientRegistration> {
    private Map<String, ClientRegistration> registrations;

    @Autowired
    private ServletContext servletContext;

    public LSFClientRegistrationRepository() {
    }

    @Override
    public synchronized ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return getRegistrations().get(registrationId);
    }

    private Map<String, ClientRegistration> getRegistrations() {
        if (registrations != null) {
            return registrations;
        }
        HttpServletRequest request = LSFRemoteAuthenticationProvider.getHttpServletRequest();
        List<OAuth2Credentials> clientCredentials;
        List<ClientRegistration> clientRegistrations = new ArrayList<>();
        try {
            clientCredentials = runRequest(request, new LogicsRunnable<List<OAuth2Credentials>>() {
                @Override
                public List<OAuth2Credentials> run(LogicsSessionObject sessionObject) throws RemoteException {
                        return sessionObject.remoteLogics.getOauth2ClientCredentials(servletContext.getInitParameter(OAuth2ToLSFTokenFilter.AUTH_SECRET_KEY));
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        for (OAuth2Credentials clientCredential : clientCredentials) {
            if (clientCredential.getClientId() != null && clientCredential.getClientSecret() != null) {
                clientRegistrations.add(ClientRegistration.withRegistrationId(clientCredential.getRegistrationId())
                        .clientAuthenticationMethod(new ClientAuthenticationMethod(clientCredential.getClientAuthenticationMethod()))
                        .scope(clientCredential.getScope().split(" "))
                        .authorizationUri(clientCredential.getAuthorizationUri())
                        .tokenUri(clientCredential.getTokenUri())
                        .jwkSetUri(clientCredential.getJwkSetUri())
                        .userInfoUri(clientCredential.getUserInfoUri())
                        .userNameAttributeName(clientCredential.getUserNameAttributeName())
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .clientName(clientCredential.getClientName())
                        .clientId(clientCredential.getClientId())
                        .clientSecret(clientCredential.getClientSecret())
                        .redirectUriTemplate(clientCredential.getRedirectUrl()).build());
            }
        }
        return this.registrations = createRegistrationsMap(clientRegistrations);
    }

    private Map<String, ClientRegistration> createRegistrationsMap(List<ClientRegistration> registrations) {
        ConcurrentHashMap<String, ClientRegistration> result = new ConcurrentHashMap<>();
        for (ClientRegistration registration : registrations) {
            if (result.containsKey(registration.getRegistrationId())) {
                throw new IllegalStateException(String.format("Duplicate key %s",
                        registration.getRegistrationId()));
            }
            result.put(registration.getRegistrationId(), registration);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    @NonNull
    public Iterator<ClientRegistration> iterator() throws AuthenticationException {
        if (getRegistrations() == null || getRegistrations().size() == 0){
            return Collections.<String, ClientRegistration>emptyMap().values().iterator();
        }
        return this.registrations.values().iterator();
    }
}
