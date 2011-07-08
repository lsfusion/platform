package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.client.logics.ClientForm;
import platform.client.logics.ClientFormChanges;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.view.GForm;
import platform.interop.form.RemoteFormInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.SecureRandom;

public abstract class FormActionHandler<A extends Action<GetFormResult>> extends FormServiceActionHandler<A, GetFormResult> {
    public FormActionHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    protected GetFormResult createResult(RemoteFormInterface remoteForm) throws IOException {
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        ClientFormChanges clientChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(remoteForm.getRemoteChanges().form)), clientForm, null);

        String formSessionID = generateRandomSID();

        GForm gwtForm = clientForm.getGwtForm();
        gwtForm.changes = clientChanges.getGwtFormChangesDTO();
        gwtForm.sessionID = formSessionID;

        getSession().setAttribute(formSessionID, new FormSessionObject(clientForm, remoteForm));

        return new GetFormResult(gwtForm);
    }

    private String generateRandomSID() {
        String sid = "";
        do {
            sid = randomString(20);
        } while (getSession().getAttribute(sid) != null);
        return sid;
    }

    private static final String randomsymbols = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final static SecureRandom random = new SecureRandom();

    private static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomsymbols.charAt(random.nextInt(randomsymbols.length())));
        }
        return sb.toString();
    }
}
