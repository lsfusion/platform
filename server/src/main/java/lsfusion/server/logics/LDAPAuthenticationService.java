package lsfusion.server.logics;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class LDAPAuthenticationService {

    private String server;
    private Integer port;
    private String baseDN;
    private String userDNSuffix;

    public LDAPAuthenticationService(String server, Integer port, String baseDN, String userDNSuffix) {
        this.server = server;
        this.port = port;
        this.baseDN = baseDN;
        this.userDNSuffix = userDNSuffix;
    }

    public LDAPParameters authenticate(String username, String password) throws CommunicationException {

        server = server == null ? "localhost" : server.trim();
        port = port == null ? 389 : port;
        
        Hashtable<String, String> environment = new Hashtable<>();

        String principal = username + (userDNSuffix != null ? userDNSuffix : "");
        
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, "ldap://" + server + ":" + port);
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, principal);
        environment.put(Context.SECURITY_CREDENTIALS, password);

        DirContext authContext = null;
        try {
            authContext = new InitialDirContext(environment);
            // user is authenticated

            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String firstName = null;
            String lastName = null;
            String email = null;
            List<String> groupNames = new ArrayList<>();

            if (baseDN != null) {
                NamingEnumeration personResults = authContext.search(baseDN, "(userPrincipalName=" + principal + ")", controls);
                while (personResults.hasMore()) {
                    SearchResult searchResult = (SearchResult) personResults.next();

                    Attribute givenName = searchResult.getAttributes().get("givenName");
                    if (givenName != null)
                        firstName = (String) givenName.get();

                    Attribute sn = searchResult.getAttributes().get("sn");
                    if (sn != null)
                        lastName = (String) sn.get();
                    else {
                        Attribute name = searchResult.getAttributes().get("name");
                        if (name != null)
                            lastName = (String) name.get();
                    }

                    Attribute mail = searchResult.getAttributes().get("mail");
                    if (mail != null)
                        email = (String) mail.get();

                    Attribute memberOf = searchResult.getAttributes().get("memberOf");
                    if (memberOf != null) {
                        NamingEnumeration memberOfAll = memberOf.getAll();
                        while (memberOfAll.hasMore()) {
                            String memberString = (String) memberOfAll.next();
                            String[] members = memberString.split(",");
                            for (String member : members) {
                                if (member.startsWith("CN=")) {
                                    groupNames.add(member.replace("CN=", ""));
                                }
                            }
                        }
                    }
                }
            }


            return new LDAPParameters(true, firstName, lastName, email, groupNames);
        } catch (CommunicationException e) {
            throw e;
        } catch (AuthenticationException e) {
            return new LDAPParameters(false, null, null, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Error while authenticating using LDAP : ", e);
        } finally {
            if (authContext != null) {
                try {
                    authContext.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
