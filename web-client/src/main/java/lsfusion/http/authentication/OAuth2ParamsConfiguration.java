package lsfusion.http.authentication;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.http.controller.LogicsRequestHandler;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OAuth2ParamsConfiguration extends LogicsRequestHandler {
    private static final List<String> clients = Arrays.asList("google", "github");
    private static final String authSecretKey = "authSecret";
    private String authSecret;

    @Autowired
    private ServletContext servletContext;

    public OAuth2ParamsConfiguration() {
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        authSecret = servletContext.getInitParameter(authSecretKey);
        List<ClientRegistration> registrations = clients.stream()
                .map(c -> getRegistration(c))
                .filter(registration -> registration != null)
                .collect(Collectors.toList());

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration getRegistration(String client){
        HttpServletRequest request = null;
        final RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs != null){
            request = ((ServletRequestAttributes) attribs).getRequest();
        }
        Pair<String, String> clientCredentials;
        try {
            clientCredentials = runRequest(request, new LogicsRunnable<Pair<String, String>>() {
                @Override
                public Pair<String, String> run(LogicsSessionObject sessionObject) throws RemoteException {
                    return sessionObject.remoteLogics.getOauth2ClientCredentials(client, authSecret);
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        String clientId = clientCredentials.first;
        String clientSecret = clientCredentials.second;

        if (client.equals("google")) {
            return CommonOAuth2Provider.GOOGLE.getBuilder(client)
                    .clientId(clientId).clientSecret(clientSecret).build();
        }
        if (client.equals("github")) {
            return CommonOAuth2Provider.GITHUB.getBuilder(client)
                    .clientId(clientId).clientSecret(clientSecret).build();
        }
        return null;
    }
}
