package lsfusion.client.form.property.async;

import java.util.List;

public class ClientInputListAction {
    public String action;
    public ClientAsyncExec asyncExec;
    public List<ClientQuickAccess> quickAccessList;

    public ClientInputListAction(String action, ClientAsyncExec asyncExec, List<ClientQuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.quickAccessList = quickAccessList;
    }
}