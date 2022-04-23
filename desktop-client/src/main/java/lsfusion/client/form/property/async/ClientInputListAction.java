package lsfusion.client.form.property.async;

import lsfusion.interop.form.event.BindingMode;

import javax.swing.*;
import java.util.List;

public class ClientInputListAction {
    public String action;
    public ClientAsyncEventExec asyncExec;
    public KeyStroke keyStroke;
    public BindingMode editingBindingMode;
    public List<ClientQuickAccess> quickAccessList;

    public ClientInputListAction(String action, ClientAsyncEventExec asyncExec, KeyStroke keyStroke, BindingMode editingBindingMode, List<ClientQuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.editingBindingMode = editingBindingMode;
        this.quickAccessList = quickAccessList;
    }
}