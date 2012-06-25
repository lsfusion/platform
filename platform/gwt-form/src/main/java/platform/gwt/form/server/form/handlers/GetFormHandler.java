package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientForm;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.view.GForm;
import platform.interop.form.RemoteFormInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.SecureRandom;

public class GetFormHandler extends FormActionHandler<GetForm, GetFormResult> {
    public GetFormHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult executeEx(GetForm action, ExecutionContext context) throws DispatchException, IOException {
        String sid = action.sid != null && !action.sid.isEmpty()
                     ? action.sid
                     : "connectionsForm";

        return createResult(servlet.getNavigator().createForm(sid, action.initialObjects, false, false, true));
    }

    protected GetFormResult createResult(RemoteFormInterface remoteForm) throws IOException {
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        String formSessionID = generateRandomSID();

        GForm gwtForm = clientForm.getGwtForm();
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
