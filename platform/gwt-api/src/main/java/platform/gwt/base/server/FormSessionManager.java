package platform.gwt.base.server;

import org.springframework.beans.factory.annotation.Autowired;
import platform.client.logics.ClientForm;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.view.GForm;
import platform.interop.form.RemoteFormInterface;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.SecureRandom;

public class FormSessionManager {
    @Autowired
    private HttpSession session;

    public FormSessionManager() {}

    public GForm createFormAndPutInSession(RemoteFormInterface remoteForm) throws IOException {
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        String formSessionID = generateRandomSID();

        GForm gForm = clientForm.getGwtForm();
        gForm.sessionID = formSessionID;

        session.setAttribute(formSessionID, new FormSessionObject(clientForm, remoteForm));

        return gForm;
    }

    private String generateRandomSID() {
        String sid = "";
        do {
            sid = randomString(20);
        } while (session.getAttribute(sid) != null);
        return sid;
    }

    private static final char[] randomsymbols = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final static SecureRandom random = new SecureRandom();
    private static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomsymbols[random.nextInt(randomsymbols.length)]);
        }
        return sb.toString();
    }


}
