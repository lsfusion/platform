package lsfusion.interop.action;

import lsfusion.interop.form.FormClientData;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends ExecuteClientAction {

    public RemoteFormInterface remoteForm;

    public FormClientData clientData;

    public ShowFormType showFormType;
    public boolean forbidDuplicate;
    public boolean syncType;
    public String formId;

    public FormClientAction(boolean forbidDuplicate, boolean syncType, RemoteFormInterface remoteForm, FormClientData clientData, ShowFormType showFormType, String formId) {
        this.clientData = clientData;
        this.remoteForm = remoteForm;
        this.showFormType = showFormType;
        this.forbidDuplicate = forbidDuplicate;
        this.syncType = syncType;
        this.formId = formId;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "FormClientAction[showFormType: " + showFormType + "]";
    }
}
