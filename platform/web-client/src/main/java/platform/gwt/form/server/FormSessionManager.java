package platform.gwt.form.server;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import platform.client.logics.ClientForm;
import platform.client.logics.ClientFormChanges;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.InvalidateListener;
import platform.gwt.form.server.convert.ClientComponentToGwtConverter;
import platform.gwt.form.server.convert.ClientFormChangesToGwtConverter;
import platform.gwt.form.shared.view.GForm;
import platform.interop.RemoteLogicsInterface;
import platform.interop.action.ProcessFormChangesClientAction;
import platform.interop.form.RemoteFormInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

public class FormSessionManager implements InitializingBean, DisposableBean, InvalidateListener {
    @Autowired
    private BusinessLogicsProvider blProvider;

    private int nextFormId = 0;
    private final Map<String, FormSessionObject> currentForms = synchronizedMap(new HashMap<String, FormSessionObject>());

    public FormSessionManager() {}

    public GForm createForm(RemoteFormInterface remoteForm, LogicsDispatchServlet<RemoteLogicsInterface> servlet) throws IOException {
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        GForm gForm = new ClientComponentToGwtConverter().convertOrCast(clientForm);
        gForm.sessionID = nextFormSessionID();

        ProcessFormChangesClientAction clientAction = (ProcessFormChangesClientAction) remoteForm.getRemoteChanges(-1).actions[0];
        gForm.initialFormChanges = ClientFormChangesToGwtConverter.getInstance().convertFormChanges(new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(clientAction.formChanges)), clientForm));
        currentForms.put(gForm.sessionID, new FormSessionObject(clientForm, remoteForm));

        return gForm;
    }

    private String nextFormSessionID() {
        return "form" + nextFormId++ ;
    }

    @Override
    public void onInvalidate() {
        cleanSessionForms();
    }

    private void cleanSessionForms() {
        currentForms.clear();
    }

    public FormSessionObject getFormSessionObject(String formSessionID) {
        FormSessionObject formObject = currentForms.get(formSessionID);

        if (formObject == null) {
            throw new RuntimeException("Форма не найдена.");
        }

        return formObject;
    }

    public FormSessionObject removeFormSessionObject(String formSessionID) {
        return currentForms.remove(formSessionID);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(blProvider, "businessLogicProvider must be specified");
        blProvider.addInvlidateListener(this);
    }

    @Override
    public void destroy() throws Exception {
        blProvider.removeInvlidateListener(this);
    }
}
