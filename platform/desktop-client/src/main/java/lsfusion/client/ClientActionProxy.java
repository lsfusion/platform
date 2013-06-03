package lsfusion.client;

import lsfusion.client.form.ClientFormController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

public class ClientActionProxy implements Action {

    private final Action a;
    private final ClientFormController form;

    public ClientActionProxy(ClientFormController form, Action a) {
        this.form = form;
        this.a = a;
    }

    public Object getValue(String key) {
        return a.getValue(key);
    }

    public void putValue(String key, Object value) {
        a.putValue(key, value);
    }

    public void setEnabled(boolean b) {
        a.setEnabled(b);
    }

    public boolean isEnabled() {
        return a.isEnabled();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        a.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        a.removePropertyChangeListener(listener);
    }

    public void actionPerformed(ActionEvent e) {
        if (form.commitCurrentEditing()) {
            a.actionPerformed(e);
        }
    }
}
