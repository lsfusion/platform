package lsfusion.client;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

public class ContainerFocusListener implements PropertyChangeListener {

    public static void addListener(Container container, FocusListener listener) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new ContainerFocusListener(container, listener));
   }

    private WeakReference<Container> containerRef;
    private WeakReference<FocusListener> listenerRef;
    private boolean hasFocus = false;

    public ContainerFocusListener(Container container, FocusListener listener) {
        this.containerRef = new WeakReference(container);
        this.listenerRef = new WeakReference(listener);
    }

    public void propertyChange(PropertyChangeEvent evt) {

        Container container = containerRef.get();
        FocusListener listener = listenerRef.get();

        if (container == null || listener == null) { // сборщик мусора собрал объекты
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(this);
            return;
        }

        Component focusComponent = (Component)evt.getNewValue();
        if (focusComponent != null) {
            boolean newHasFocus = (container.isAncestorOf(focusComponent)) | (focusComponent.equals(container));
            if (hasFocus != newHasFocus) {
                hasFocus = newHasFocus;
                if (hasFocus) {
                    listener.focusGained(new FocusEvent(focusComponent, FocusEvent.FOCUS_GAINED));
                }
            }
        }
    }
}
