package platform.client;

import com.google.common.collect.ForwardingMap;
import platform.base.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

class FocusOwnerTracer implements PropertyChangeListener {
    private static final String FOCUS_OWNER_PROPERTY = "focusOwner";
    private static final String PEMANENT_FOCUS_OWNER_PROPERTY = "permanentFocusOwner";
    private static final String CURRENT_CYCLE_ROOT_PROPERTY = "currentFocusCycleRoot";
    private final KeyboardFocusManager focusManager;

    private FocusOwnerTracer() {
        this.focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    }

    void startListening() {
        if (focusManager != null) {
            focusManager.addPropertyChangeListener(FOCUS_OWNER_PROPERTY, this);
            focusManager.addPropertyChangeListener(PEMANENT_FOCUS_OWNER_PROPERTY, this);
            focusManager.addPropertyChangeListener(CURRENT_CYCLE_ROOT_PROPERTY, this);
        }
    }

    public void stopListening() {
        if (focusManager != null) {
            focusManager.removePropertyChangeListener(FOCUS_OWNER_PROPERTY, this);
            focusManager.removePropertyChangeListener(PEMANENT_FOCUS_OWNER_PROPERTY, this);
            focusManager.removePropertyChangeListener(CURRENT_CYCLE_ROOT_PROPERTY, this);
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(CURRENT_CYCLE_ROOT_PROPERTY)) {
            cycleRootChanged(e);
        } else {
            focusOwnerChanged(e);
        }
    }

    private void cycleRootChanged(PropertyChangeEvent e) {
        System.out.println("-------------------------------------------------");
        System.out.println("        cycleRoot changed: ");
        System.out.println("            o: " + e.getOldValue());
        System.out.println("            n: " + e.getNewValue());
    }

    private void focusOwnerChanged(PropertyChangeEvent e) {
        Component oldOwner = (Component) e.getOldValue();
        Component newOwner = (Component) e.getNewValue();
        System.out.println("-------------------------------------------------");
        if (e.getPropertyName().equals(FOCUS_OWNER_PROPERTY)) {
            System.out.println("        focusOwner changed: ");
        } else {
            System.out.println("        permanentFocusOwner changed: ");
        }
        System.out.println("            o: " + oldOwner);
        System.out.println("            n: " + newOwner);

        ExceptionUtils.dumpStack();

        if (oldOwner instanceof JComponent) {
            JComponent oldJComp = (JComponent) oldOwner;
            Object border = oldJComp.getClientProperty("tttOldBorder");
            if (border != null) {
                oldJComp.setBorder(unwrapNull(border));
                oldJComp.repaint();
            }
        }

        if (newOwner instanceof JComponent) {
            JComponent newJComp = (JComponent) newOwner;
            if (newJComp.getClientProperty("tttOldBorder") == null) {
                newJComp.putClientProperty("tttOldBorder", wrapNull(newJComp.getBorder()));
            }
            newJComp.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            newJComp.repaint();
        }
    }

    private static final Object NULL = new Object();

    private Object wrapNull(Border border) {
        return border == null ? NULL : border;
    }

    private Border unwrapNull(Object border) {
        return border == NULL ? null : (Border) border;
    }

    protected static void installFocusTracer() {
        traceFocusOwner();

//        traceEventQueue();
//
//        traceKeyDispatcher();
//
//        traceAWTEvents();

        traceMostRecentFocusOwner();
    }

    private static void traceFocusOwner() {
        new FocusOwnerTracer().startListening();
    }

    private static void traceMostRecentFocusOwner() {
        try {
            Field mrfoField = KeyboardFocusManager.class.getDeclaredField("mostRecentFocusOwners");
            mrfoField.setAccessible(true);
            final Map delegate = (Map) mrfoField.get(null);
            Map mrfo = new ForwardingMap() {
                public Object put(Object key, Object value) {
                    System.out.println("-------------------------------------------------");
                    System.out.println("mostRecentFocusOwners updated:");

                    WeakReference ref = (WeakReference) value;
                    if (ref != null) {
                        Object comp = ref.get();
                        if (comp != null) {
                            System.out.println("                    component: " + comp);
                        }
                    }

                    ExceptionUtils.dumpStack();

                    return super.put(key, value);
                }

                protected Map delegate() {
                    return delegate;
                }
            };
            mrfoField.set(null, mrfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void traceAWTEvents() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent e) {
                System.out.println("-------------------------------------------------");
                System.out.println("AWTEventListener keyEvent : " + e);
                System.out.println("                   source : " + e.getSource());
            }
        }, AWTEvent.KEY_EVENT_MASK);

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                System.out.println("-------------------------------------------------");
                System.out.println("AWTEventListener focusEvent : " + e);
                System.out.println("                     source : " + e.getSource());
            }
        }, AWTEvent.FOCUS_EVENT_MASK);
    }

    private static void traceKeyDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                System.out.println("-------------------------------------------------");
                System.out.println("KeyEventDispatcher keyEvent : " + e);
                System.out.println("                     source : " + e.getSource());
                return false;
            }
        });
    }

    private static void traceEventQueue() {
        EventQueue queue = new EventQueue() {
            @Override
            protected void dispatchEvent(AWTEvent awtEvent) {
                if (awtEvent instanceof KeyEvent) {
                    KeyEvent e = (KeyEvent) awtEvent;
                    System.out.println("-------------------------------------------------");
                    System.out.println("EventQueue awtEvent : " + e);
                    System.out.println("             source : " + e.getSource());
                }

                super.dispatchEvent(awtEvent);
            }
        };
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(queue);
    }
}
