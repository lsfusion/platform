package platform.client;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class FocusOwnerTracer implements PropertyChangeListener {

    private static final String FOCUS_OWNER_PROPERTY = "permanentFocusOwner";
    private final KeyboardFocusManager focusManager;

    protected static void installFocusTracer() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.
            getCurrentKeyboardFocusManager();
        new FocusOwnerTracer(focusManager);
    }

    private FocusOwnerTracer(KeyboardFocusManager focusManager) {
        this.focusManager = focusManager;
        startListening();
    }

    void startListening() {
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
        Component newFocusComponent = (Component)e.getNewValue();
        if (newFocusComponent instanceof JComponent) {
            JComponent jComp = (JComponent) newFocusComponent;
            setNewComponent(jComp);
        }
        Main.frame.setTitle(newFocusComponent == null ? "null!!" : newFocusComponent.getClass().getName() + ": " + newFocusComponent.toString());

//        Component oldOwner = (Component) e.getOldValue();
//        Component newOwner = (Component) e.getNewValue();
//        System.out.println("focusOwner changed: ");
//        System.out.println(" o: " + oldOwner);
//        System.out.println(" n: " + newOwner);
    }

    private JComponent lastComponent;
    private Border originalBorder;
    private Border tagBorder = BorderFactory.createLineBorder(Color.RED, 3);

    private void setNewComponent(JComponent newComponent) {
        if (lastComponent != null) {
            lastComponent.setBorder(originalBorder);
        }

        if (newComponent != null) {
            lastComponent = newComponent;

            originalBorder = lastComponent.getBorder();
            lastComponent.setBorder(tagBorder);
        }
    }
}
