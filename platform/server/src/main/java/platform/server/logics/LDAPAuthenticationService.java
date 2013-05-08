package platform.server.logics;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Hashtable;

public class LDAPAuthenticationService {

    private String server;
    private Integer port;

    public LDAPAuthenticationService(String server, Integer port) {
        this.server = server;
        this.port = port;
    }

    public LDAPParameters authenticate(String username, String password) throws CommunicationException {

        server = server == null ? "localhost" : server.trim();
        port = port == null ? 389 : port;

        String base = "ou=people,dc=maxcrc,dc=com";
        String dn = "cn=" + username + "," + base;
        String ldapURL = "ldap://" + server + ":" + port;

        Hashtable<String, String> environment = new Hashtable<String, String>();

        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, ldapURL);
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, dn);
        environment.put(Context.SECURITY_CREDENTIALS, password);

        try {
            DirContext authContext = new InitialDirContext(environment);
            // user is authenticated

            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration personResults = authContext.search(dn, "(objectClass=Person)", controls);
            String personName = null;
            while (personResults.hasMore()) {
                SearchResult searchResult = (SearchResult) personResults.next();
                personName = (String) searchResult.getAttributes().get("cn").get();
            }

            NamingEnumeration groupResults = authContext.search(base, "(objectClass=GroupOfNames)", controls);
            while (groupResults.hasMore()) {
                SearchResult searchResult = (SearchResult) groupResults.next();
                Attributes attributes = searchResult.getAttributes();
                String groupName = (String) attributes.get("cn").get();
                String[] members = ((String) attributes.get("member").get()).split(",");
                for (String member : members) {
                    if (member.startsWith("cn=") && member.replace("cn=", "").equals(personName))
                        return new LDAPParameters(true, groupName);
                }
            }

            return new LDAPParameters(true);
        } catch (CommunicationException e) {
            throw e;
        } catch (Exception e) {
            return new LDAPParameters(false);
        }
    }
}
