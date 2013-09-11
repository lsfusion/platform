package lsfusion.client;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

public class CaptureKeyEventsDispatcher implements KeyEventDispatcher, FocusListener {
    private final static CaptureKeyEventsDispatcher instance = new CaptureKeyEventsDispatcher();

    public static CaptureKeyEventsDispatcher get() {
        return instance;
    }

    private Component capture;

    private CaptureKeyEventsDispatcher() {
    }

    private void install() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    private void uninstall() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    }

    public void setCapture(final Component newCapture) {
        assert EventQueue.isDispatchThread();

        if (capture != null) {
            capture.removeFocusListener(this);
            if (newCapture == null) {
                uninstall();
            }
        }

        if (newCapture != null) {
            newCapture.addFocusListener(this);
            if (capture == null) {
                install();
            }
        }

        capture = newCapture;
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (e.getSource() == capture) {
            setCapture(null);
        }
    }

    @Override
    public void focusLost(FocusEvent e) { }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (capture != null) {
            e.setSource(capture);
        }
        return false;
    }
}
