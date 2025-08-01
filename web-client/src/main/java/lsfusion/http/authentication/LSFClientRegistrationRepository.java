package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.http.controller.MainController;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalRequest;
import org.json.JSONArray;
import org.json.JSONObject;
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

    private final ServletContext servletContext;

    public LSFClientRegistrationRepository(LogicsProvider logicsProvider, ServletContext servletContext) {
        super(logicsProvider);
        this.servletContext = servletContext;
    }

    @Override
    public synchronized ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return getRegistrations().get(registrationId);
    }

    private Map<String, ClientRegistration> getRegistrations() {
        if (registrations == null) {
            HttpServletRequest request = LSFRemoteAuthenticationProvider.getHttpServletRequest();
            List<ClientRegistration> clientRegistrations;
            try {
                clientRegistrations = runRequest(request, (sessionObject, retry) -> getOauth2ClientCredentials(sessionObject, request));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            registrations = createRegistrationsMap(clientRegistrations);
        }
        return registrations;
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

    private List<ClientRegistration> getOauth2ClientCredentials(LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException {
        String authSecret = servletContext.getInitParameter(OAuth2ToLSFTokenFilter.AUTH_SECRET_KEY);
        List<ClientRegistration> clientRegistrations = new ArrayList<>();

        JSONArray jsonArray = new JSONArray(MainController.sendRequest(request, ListFact.singleton(ExternalRequest.getSystemParam(authSecret)), sessionObject, "Authentication.getClientCredentials"));

        int jsonArrayLength = jsonArray.length();
        if (jsonArrayLength == 1 && jsonArray.getJSONObject(0).has("error")){
            throw new AuthenticationException(jsonArray.getJSONObject(0).getString("error"));
        }

        for (int i = 0; i < jsonArrayLength; i++) {
            JSONObject jsonobject = jsonArray.getJSONObject(i);
            clientRegistrations.add(ClientRegistration.withRegistrationId(jsonobject.getString("id"))
                    .clientAuthenticationMethod(new ClientAuthenticationMethod(jsonobject.getString("clientAuthenticationMethod")))
                    .scope(jsonobject.getString("scope").split(" "))
                    .authorizationUri(jsonobject.getString("authorizationUri"))
                    .tokenUri(jsonobject.getString("tokenUri"))
                    .jwkSetUri(jsonobject.has("jwkSetUri") ? jsonobject.getString("jwkSetUri") : null)
                    .userInfoUri(jsonobject.getString("userInfoUri"))
                    .userNameAttributeName(jsonobject.getString("userNameAttributeName"))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .clientName(jsonobject.getString("clientName"))
                    .clientId(jsonobject.getString("clientId"))
                    .clientSecret(jsonobject.getString("clientSecret"))
                    .redirectUriTemplate("{baseUrl}/{action}/oauth2/code/{registrationId}").build());
        }
        return clientRegistrations;
    }

    @Override
    @NonNull
    public Iterator<ClientRegistration> iterator() throws AuthenticationException {
        Map<String, ClientRegistration> registrations = getRegistrations();
        if (registrations == null || registrations.size() == 0){
            return Collections.<String, ClientRegistration>emptyMap().values().iterator();
        }
        return registrations.values().iterator();
    }
}
