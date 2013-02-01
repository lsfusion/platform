package platform.gwt.form.server;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import platform.client.logics.ClientForm;
import platform.client.serialization.ClientSerializationPool;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.InvalidateListener;
import platform.gwt.form.server.convert.ClientComponentToGwtConverter;
import platform.gwt.form.shared.view.GForm;
import platform.interop.RemoteLogicsInterface;
import platform.interop.form.RemoteFormInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FormSessionManager implements InitializingBean, DisposableBean, InvalidateListener {
    @Autowired
    private BusinessLogicsProvider blProvider;

    private int nextFormId = 0;
    private final Map<String, FormSessionObject> currentForms = new HashMap<String, FormSessionObject>();

    public FormSessionManager() {}

    public GForm createForm(RemoteFormInterface remoteForm, LogicsDispatchServlet<RemoteLogicsInterface> servlet) throws IOException {
        ClientForm clientForm = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        GForm gForm = new ClientComponentToGwtConverter().convertOrCast(clientForm);
        gForm.sessionID = nextFormSessionID();

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

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(blProvider, "businessLogicProvider must be specified");
        blProvider.addInvlidateListener(this);
    }

    @Override
    public void destroy() throws Exception {
        blProvider.removeInvlidateListener(this);
    }

    public FormSessionObject getFormSessionObject(String formSessionID) {
        FormSessionObject formObject = currentForms.get(formSessionID);

        if (formObject == null) {
            throw new RuntimeException("Форма не найдена.");
        }

        return formObject;
    }
}
