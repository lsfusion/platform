package lsfusion.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class MultiAction implements Action {

    ArrayList<Action> actionList = new ArrayList<>();

    public void actionPerformed(ActionEvent e) {
        for (Action action : actionList) {
            action.actionPerformed(e);
        }
    }

    public void addAction(Action a) {
        actionList.add(a);
    }

    public MultiAction(Action a) {
        actionList.add(a);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public boolean isEnabled() {
        return true;
    }

    public void setEnabled(boolean b) {
    }

    public void putValue(String key, Object value) {

    }

    public Object getValue(String key) {
        return null;
    }
}
