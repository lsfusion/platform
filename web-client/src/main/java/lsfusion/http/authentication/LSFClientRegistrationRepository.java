package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.interop.connection.authentication.OAuth2Clients;
import lsfusion.interop.connection.authentication.OAuth2Credentials;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class LSFClientRegistrationRepository extends LogicsRequestHandler implements ClientRegistrationRepository, Iterable<ClientRegistration> {
    private final Set<OAuth2Clients> clients = EnumSet.allOf(OAuth2Clients.class);
    private final Set<CommonOAuth2Provider> providers = EnumSet.allOf(CommonOAuth2Provider.class);
    private static final String authSecretKey = "authSecret";
    private Map<String, ClientRegistration> registrations;

    @Autowired
    private ServletContext servletContext;

    public LSFClientRegistrationRepository() {
    }

    @Override
    public synchronized ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return this.registrations.get(registrationId);
    }

    private ClientRegistration getRegistration(String client){
        OAuth2Credentials clientCredentials = getCredentialsFromServer(client);
        if (clientCredentials == null){
            return null;
        }
        String clientId = clientCredentials.getClientId();
        String clientSecret = clientCredentials.getClientSecret();

        return providers.stream()
                .filter(provider -> provider.toString().equalsIgnoreCase(client))
                .map(p -> p.getBuilder(client.toLowerCase()).clientId(clientId).clientSecret(clientSecret).build())
                .collect(toSingleObject());
    }

    private OAuth2Credentials getCredentialsFromServer(String client) {
        HttpServletRequest request = null;
        final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs != null){
            request = ((ServletRequestAttributes) attribs).getRequest();
        }
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

    private static <T> Collector<T, ?, T> toSingleObject() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        return null;
                    }
                    return list.get(0);
                }
        );
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        List<ClientRegistration> clientRegistrations = clients.stream()
                .map(c -> getRegistration(c.client))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        this.registrations = createRegistrationsMap(clientRegistrations);
        return this.registrations.values().iterator();
    }
}
