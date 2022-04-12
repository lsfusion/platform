package lsfusion.client.form.property.async;

import javax.swing.*;
import java.util.List;

public class ClientInputListAction {
    public String action;
    public ClientAsyncEventExec asyncExec;
    public KeyStroke keyStroke;
    public List<ClientQuickAccess> quickAccessList;

    public ClientInputListAction(String action, ClientAsyncEventExec asyncExec, KeyStroke keyStroke, List<ClientQuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.quickAccessList = quickAccessList;
    }
}