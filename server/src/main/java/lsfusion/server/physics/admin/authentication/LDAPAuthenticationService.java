package lsfusion.server.physics.admin.authentication;

import lsfusion.base.BaseUtils;

import javax.naming.*;
import javax.naming.directory.*;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.Principal;
import java.util.*;

public class LDAPAuthenticationService implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;

    private boolean success;
    private final Set<Principal> principals = new HashSet<>();

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = options;
    }

    private String getOptionAsString(String key, String defaultValue) {
        Object value = options.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    public static class UserPrincipal implements Principal {
        private final String name;
        private final Object value;
        public UserPrincipal(String name, Object value) {
            this.name = name;
            this.value = value;
        }
        public Object getValue() {
            return value;
        }
        @Override
        public String getName() {
            return name;
        }
    }
    public static class UserAttributePrincipal implements Principal {
        private final String name;
        private final String value;
        public UserAttributePrincipal(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String getValue() {
            return value;
        }
        @Override
        public String getName() {
            return name;
        }
    }

    public static String getRealm(String dn) {
        if (dn == null)
            return null;

        StringBuilder realm = new StringBuilder();
        for (String p : dn.split(",")) {
            p = p.trim();
            if (p.toUpperCase().startsWith("DC=")) {
                if (realm.length() > 0)
                    realm.append('.');
                realm.append(p.substring(3));
            }
        }
        return realm.toString().toUpperCase(); // Kerberos needs UpperCase
    }

    @Override
    public boolean login() throws LoginException {
        try {
            NameCallback nameCb = new NameCallback("Username: ");
            PasswordCallback passCb = new PasswordCallback("Password: ", false);
            callbackHandler.handle(new Callback[]{nameCb, passCb});

            String baseDN = getOptionAsString("baseDN", null);
            String principal = nameCb.getName() + getOptionAsString("userDNSuffix", "");
            String password = new String(passCb.getPassword());
            String server = getOptionAsString("server", "localhost");
            String providerURL = "ldap://" + server + ":" + Integer.parseInt(getOptionAsString("port", "389"));

            InitialDirContext authContext = null;
            try {
                if ((boolean) options.get("useServiceUser")) {
                    Configuration jaasConfig = new Configuration() {
                        @Override
                        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                            Map<String, String> options = new HashMap<>();
                            options.put("useTicketCache", "false");
                            options.put("doNotPrompt", "false");
                            options.put("storeKey", "false");
                            options.put("useKeyTab", "false");
                            options.put("refreshKrb5Config", "true");
                            options.put("isInitiator", "true");
                            options.put("principal", principal);

                            return new AppConfigurationEntry[] {
                                    new AppConfigurationEntry(
                                            "com.sun.security.auth.module.Krb5LoginModule",
                                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                            options)
                            };
                        }
                    };

                    CallbackHandler callbackHandler = callbacks -> {
                        for (Callback cb : callbacks) {
                            if (cb instanceof NameCallback)
                                ((NameCallback) cb).setName(principal);
                            else if (cb instanceof PasswordCallback)
                                ((PasswordCallback) cb).setPassword(password.toCharArray());
                        }
                    };
                    new LoginContext("KerberosLogin", null, callbackHandler, jaasConfig).login();

                    authContext = new InitialDirContext(getEnv(getOptionAsString("serviceUser", null),
                            getOptionAsString("serviceUserPassword", null), providerURL));
                    readAttributes(authContext, baseDN, principal);

                } else {
                    if (baseDN != null) {
                        authContext = new InitialDirContext(getEnv(principal, password, providerURL));
                        readAttributes(authContext, baseDN, principal);
                    }
                }
            } finally {
                if (authContext != null)
                    authContext.close();
            }

            success = true;
            return true;

//            because this method can only throw LoginException
        } catch (CommunicationException ce) { //no connection to AD
            throw new LoginException(ce.getMessage());
        } catch (NamingException ne) { // incorrect login or password
            throw new FailedLoginException(ne.getMessage());
        } catch (LoginException le) { //useServiceUser
            if (le.getCause() instanceof SocketTimeoutException) //no connection to Kerberos
                throw le;
            else
                throw new FailedLoginException(le.getMessage()); // incorrect login or password
        } catch (IOException | UnsupportedCallbackException e) {
            return false;
        }
    }

    private Hashtable<String, String> getEnv(String principal, String password, String providerURL) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);

        return env;
    }

    private void readAttributes(DirContext authContext, String DN, String principal) throws NamingException {
        NamingEnumeration<SearchResult> personResults = null;
        try {
            List<String> groupNames = new ArrayList<>();
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            personResults =
                    authContext.search(DN, "(userPrincipalName=" + principal + ")", controls);
            while (personResults.hasMore()) {
                SearchResult searchResult = personResults.next();

                Attribute givenName = searchResult.getAttributes().get("givenName");
                if (givenName != null)
                    principals.add(new UserPrincipal("firstName", givenName.get()));

                Attribute sn = searchResult.getAttributes().get("sn");
                if (sn != null) {
                    principals.add(new UserPrincipal("lastName", sn.get()));
                } else {
                    Attribute name = searchResult.getAttributes().get("name");
                    if (name != null)
                        principals.add(new UserPrincipal("lastName", name.get()));
                }

                Attribute mail = searchResult.getAttributes().get("mail");
                if (mail != null)
                    principals.add(new UserPrincipal("email", mail.get()));

                Attribute memberOf = searchResult.getAttributes().get("memberOf");
                if (memberOf != null) {
                    NamingEnumeration<?> memberOfAll = memberOf.getAll();
                    while (memberOfAll.hasMore()) {
                        String memberString = (String) memberOfAll.next();
                        String[] members = memberString.split(",");
                        for (String member : members) {
                            if (member.startsWith("CN="))
                                groupNames.add(member.replace("CN=", ""));
                        }
                    }
                }
                principals.add(new UserPrincipal("groupNames", groupNames));
                for (Attribute attribute : Collections.list(searchResult.getAttributes().getAll())) {
                    Object attributeValue = attribute.get();
                    String value = attributeValue != null ? attributeValue.toString() : null;
                    if (BaseUtils.isVisiblyValid(value))
                        principals.add(new UserAttributePrincipal(attribute.getID(), value));
                }
            }
        } catch (PartialResultException e) {
            //do nothing. not critical for AD
        } finally {
            if (personResults != null)
                personResults.close();
        }
    }

    @Override
    public boolean commit() {
        if (!success)
            return false;
        subject.getPrincipals().addAll(principals);
        return true;
    }

    @Override
    public boolean abort() {
        principals.clear();
        success = false;
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        return true;
    }
}
