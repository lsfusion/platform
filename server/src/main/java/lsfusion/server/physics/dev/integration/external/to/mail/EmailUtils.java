package lsfusion.server.physics.dev.integration.external.to.mail;

import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import java.security.GeneralSecurityException;
import java.util.Properties;

import static lsfusion.server.physics.dev.integration.external.to.mail.AccountType.*;

public class EmailUtils {

    public static Store getEmailStore(String receiveHost, AccountType accountType, boolean startTLS) throws GeneralSecurityException, NoSuchProviderException {
        Properties mailProps = new Properties();
        mailProps.setProperty(accountType == POP3 ? "mail.pop3.host" : "mail.imap.host", receiveHost);

        boolean imap = accountType == IMAP;
        boolean imaps = accountType == IMAPS;
        if (imap || imaps) { //imaps
            MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            mailProps.put(imap ? "mail.imap.ssl.socketFactory" : "mail.imaps.ssl.socketFactory", socketFactory);
            mailProps.setProperty("mail.store.protocol", accountType.getProtocol());
            mailProps.setProperty(imap ? "mail.imap.timeout" : "mail.imaps.timeout", "5000");
            if(startTLS) {
                mailProps.setProperty("mail.imap.starttls.enable", "true");
            }
            if(imaps) {
                //options to increase downloading big attachments
                mailProps.put("mail.imaps.partialfetch", "true");
                mailProps.put("mail.imaps.fetchsize", "819200");
            }
        }

        return Session.getInstance(mailProps).getStore(accountType.getProtocol());
    }
}
