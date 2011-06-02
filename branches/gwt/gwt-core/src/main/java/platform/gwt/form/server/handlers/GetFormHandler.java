package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.client.logics.ClientForm;
import platform.client.logics.ClientFormChanges;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.base.server.DebugUtil;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.view.GForm;
import platform.interop.form.RemoteFormInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.security.SecureRandom;

public class GetFormHandler extends FormServiceActionHandler<GetForm, GetFormResult> {
    public GetFormHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult execute(GetForm action, ExecutionContext context) throws DispatchException {
        String sid = action.sid != null && !action.sid.isEmpty()
                     ? action.sid
                     : "connectionsForm";

        try {
            RemoteFormInterface remoteForm = servlet.navigator.createForm(sid, false, true);
            ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

            ClientFormChanges clientChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(remoteForm.getRemoteChanges().form)), clientForm, null);

            String formSessionID = generateRandomSID();

            GForm gwtForm = clientForm.getGwtForm();
            gwtForm.changes = clientChanges.getGwtFormChangesDTO();
            gwtForm.sessionID = formSessionID;

            getSession().setAttribute(formSessionID, new FormSessionObject(clientForm, remoteForm));

            return new GetFormResult(gwtForm);
        } catch (Throwable e) {
            logger.error("Ошибка в getForm: ", e);
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
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
