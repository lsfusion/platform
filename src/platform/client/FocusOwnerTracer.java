package platform.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.*;

class FocusOwnerTracer implements PropertyChangeListener {

    public static final String FOCUS_OWNER_PROPERTY = "permanentFocusOwner";
    protected KeyboardFocusManager focusManager;

    protected static void installFocusTracer() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.
            getCurrentKeyboardFocusManager();
        new FocusOwnerTracer(focusManager);
    }

    public FocusOwnerTracer(KeyboardFocusManager focusManager) {
        this.focusManager = focusManager;
        startListening();
    }

    public void startListening() {
        if (focusManager != null) {
            focusManager.addPropertyChangeListener(FOCUS_OWNER_PROPERTY, this);
        }
    }

    public void stopListening() {
        if (focusManager != null) {
            focusManager.removePropertyChangeListener(FOCUS_OWNER_PROPERTY, this);
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        Component oldOwner = (Component) e.getOldValue();
        Component newOwner = (Component) e.getNewValue();
        System.out.print("focusOwner changed: ");
        System.out.print(" old - " + oldOwner);
        System.out.println(" new - " + newOwner);
    }

}
