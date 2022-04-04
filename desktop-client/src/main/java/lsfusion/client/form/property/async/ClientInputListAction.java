package lsfusion.client.form.property.async;

import java.util.List;

public class ClientInputListAction {
    public String action;
    public List<ClientQuickAccess> quickAccessList;

    public ClientInputListAction(String action, List<ClientQuickAccess> quickAccessList) {
        this.action = action;
        this.quickAccessList = quickAccessList;
    }
}