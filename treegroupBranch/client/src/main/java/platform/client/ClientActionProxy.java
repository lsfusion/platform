package platform.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

public class ClientActionProxy implements Action {

    Action a;

    public ClientActionProxy(Action a) {
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
        SwingUtils.commitCurrentEditing();
        a.actionPerformed(e);
    }
}
