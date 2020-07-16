package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.authentication.OAuth2Credentials;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static lsfusion.base.ApiResourceBundle.getString;

public class LSFClientRegistrationRepository extends LogicsRequestHandler implements ClientRegistrationRepository, Iterable<ClientRegistration>, InitializingBean {
    private static final String authSecretKey = "authSecret";
    private Map<String, ClientRegistration> registrations;

    @Autowired
    private ServletContext servletContext;

    public LSFClientRegistrationRepository() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<String> clients = getClientsIdsFromServer();
        if (clients != null) {
            this.registrations = createRegistrationsMap(clients.stream().map(this::getRegistration)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
    }

    @Override
    public synchronized ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return this.registrations.get(registrationId);
    }

    private List<String> getClientsIdsFromServer() {
        HttpServletRequest request = getHttpServletRequest();
        try {
            String clientsIds = runRequest(request, new LogicsRunnable<String>() {
                @Override
                public String run(LogicsSessionObject sessionObject) throws RemoteException {
                    return sessionObject.remoteLogics.getClientsIds();
                }
            });
            return clientsIds != null ? Arrays.asList(clientsIds.split(" ")) : null;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private ClientRegistration getRegistration(String client){
        OAuth2Credentials oAuth2Client = getCredentialsFromServer(client);
        if (oAuth2Client == null){
            return null;
        }

        return ClientRegistration.withRegistrationId(client.toLowerCase())
                .clientAuthenticationMethod(new ClientAuthenticationMethod(oAuth2Client.getClientAuthenticationMethod()))
                .scope(oAuth2Client.getScope().split(" "))
                .authorizationUri(oAuth2Client.getAuthorizationUri())
                .tokenUri(oAuth2Client.getTokenUri())
                .jwkSetUri(oAuth2Client.getJwkSetUri())
                .userInfoUri(oAuth2Client.getUserInfoUri())
                .userNameAttributeName(oAuth2Client.getUserNameAttributeName())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientName(oAuth2Client.getClientName())
                .clientId(oAuth2Client.getClientId())
                .clientSecret(oAuth2Client.getClientSecret())
                .redirectUriTemplate(oAuth2Client.getRedirectUrl()).build();
    }

    private OAuth2Credentials getCredentialsFromServer(String client) {
        HttpServletRequest request = getHttpServletRequest();
        OAuth2Credentials clientCredentials;
        try {
            clientCredentials = runRequest(request, new LogicsRunnable<OAuth2Credentials>() {
                @Override
                public OAuth2Credentials run(LogicsSessionObject sessionObject) throws RemoteException {
                    return sessionObject.remoteLogics.getOauth2ClientCredentials(client, servletContext.getInitParameter(authSecretKey));
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return clientCredentials;
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
        if (registrations == null || registrations.size() == 0){
            throw new AuthenticationException(getString("exceptions.incorrect.web.client.auth.token"));
        }
        return this.registrations.values().iterator();
    }

    private HttpServletRequest getHttpServletRequest() {
        HttpServletRequest request = null;
        final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs != null) {
            request = ((ServletRequestAttributes) attribs).getRequest();
        }
        return request;
    }
}
