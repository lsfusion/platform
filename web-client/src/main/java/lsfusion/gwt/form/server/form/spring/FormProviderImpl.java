package lsfusion.gwt.form.server.form.spring;

import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProviderImpl;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.interop.form.RemoteFormInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.synchronizedMap;

// session scoped - one for one browser (! not tab)
public class FormProviderImpl implements FormProvider, InitializingBean, DisposableBean, InvalidateListener {
    @Autowired
    private LogicsProvider blProvider;

    private int nextFormId = 0;
    private final Map<String, FormSessionObject> currentForms = synchronizedMap(new HashMap<String, FormSessionObject>());

    public FormProviderImpl() {}

    public GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String tabSID, LSFusionDispatchServlet servlet) throws IOException {
        String formID = nextFormSessionID();
        FormProviderImpl formProvider = this;

        GForm gForm = NavigatorProviderImpl.createForm(canonicalName, formSID, remoteForm, immutableMethods, firstChanges, tabSID, formID, formProvider);

        return gForm;
    }

    private String nextFormSessionID() {
        return "form" + nextFormId++ ;
    }

    @Override
    public void onInvalidate() {
//        cleanSessionForms();
    }

    private void cleanSessionForms() {
        currentForms.clear();
    }

    public FormSessionObject getFormSessionObject(String formSessionID) {
        FormSessionObject formObject = getFormSessionObjectOrNull(formSessionID);

        if (formObject == null) {
            throw new RuntimeException("Форма не найдена.");
        }

        return formObject;
    }

    public void addFormSessionObject(String formSessionID, FormSessionObject formSessionObject) {
        currentForms.put(formSessionID, formSessionObject);
    }

    @Override
    public FormSessionObject getFormSessionObjectOrNull(String formSessionID) {
        return currentForms.get(formSessionID);
    }

    public FormSessionObject removeFormSessionObject(String formSessionID) {
        return currentForms.remove(formSessionID);
    }

    @Override
    public void removeFormSessionObjects(String tabSID) {
        Collection<String> sessionIDs = new HashSet<>(currentForms.keySet());
        for (String sessionID : sessionIDs) {
            if (currentForms.get(sessionID).tabSID.equals(tabSID)) {
                currentForms.remove(sessionID); // по хорошему надо вызывать remoteForm.close (по аналогии с RemoteNavigator), если остались открытые вкладки (так как если их нет, всю работу выполнит RemoteNavigator.close) - но это редкий и нестандартный случай так что пока делать не будем 
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(blProvider, "businessLogicProvider must be specified");
        blProvider.addInvalidateListener(this);
    }

    @Override
    public void destroy() throws Exception {
        blProvider.removeInvalidateListener(this);
    }
}
