package lsfusion.server.physics.admin.authentication;

import lsfusion.base.BaseUtils;

import javax.naming.*;
import javax.naming.directory.*;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
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

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCb = new NameCallback("Username: ");
        PasswordCallback passCb = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(new Callback[]{nameCb, passCb});
        } catch (Exception e) {
            throw new LoginException("Error in callback: " + e.getMessage());
        }

        try {
            String baseDN = getOptionAsString("baseDN", null);
            String principal = nameCb.getName() + getOptionAsString("userDNSuffix", "");

            if ((boolean) options.get("useServiceUser")) {
                DirContext serviceCtx = new InitialDirContext(getEnv(getOptionAsString("serviceUser", null),
                        getOptionAsString("serviceUserPassword", null)));
                SearchControls controls = new SearchControls();
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                NamingEnumeration<SearchResult> results = serviceCtx.search(baseDN, "(sAMAccountName=" + nameCb.getName() + ")", controls);

                if (!results.hasMore())
                    throw new LoginException(); // user not found

                SearchResult result = results.next();
                String userDN = result.getNameInNamespace();

                if (userDN != null)
                    readAttributes(new InitialDirContext(getEnv(userDN, new String(passCb.getPassword()))), userDN, principal);
            } else {
                if (baseDN != null)
                    readAttributes(new InitialDirContext(getEnv(principal, new String(passCb.getPassword()))), baseDN, principal);
            }

            success = true;
            return true;
        } catch (CommunicationException e) {
            throw  new LDAPCommunicationException(e.getCause().getMessage());
        } catch (Exception e) {
            throw new LoginException("LDAP auth failed: " + e.getMessage());
        }
    }

    private Hashtable<String, String> getEnv(String principal, String password) {
        String server = getOptionAsString("server", "localhost");
        int port = Integer.parseInt(getOptionAsString("port", "389"));

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + server + ":" + port);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);

        return env;
    }

    private void readAttributes(DirContext authContext, String DN, String principal) throws NamingException {
        List<String> groupNames = new ArrayList<>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> personResults =
                authContext.search(DN, "(userPrincipalName=" + principal + ")", controls);

        try {
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
        }
    }

    public static class LDAPCommunicationException extends LoginException {
        public LDAPCommunicationException(String msg) {
            super(msg);
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
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        return true;
    }
}
