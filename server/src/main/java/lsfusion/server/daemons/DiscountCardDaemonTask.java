package lsfusion.server.daemons;

import lsfusion.interop.event.AbstractDaemonTask;
import lsfusion.server.ServerLoggers;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.Serializable;

public class DiscountCardDaemonTask extends AbstractDaemonTask implements Serializable, KeyEventDispatcher {

    public static final String SCANNER_SID = "SCANNER";
    //public static final String CARD_SID = "CARD";
    private static boolean recording;
    private static String input = "";

    public DiscountCardDaemonTask() {
    }

    @Override
    public void start() {
        install();
    }

    @Override
    public void stop() {
        uninstall();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if(e.getID() == KeyEvent.KEY_PRESSED) {
            if(e.getKeyChar() == ';' || e.getKeyChar() == 'ж') {
                recording = true;
                e.consume();
            }
            else if(e.getKeyChar() == '\n') {
                recording = false;
                if(input.length() > 2 && input.charAt(input.length() - 2) == 65535
                        && ((input.charAt(input.length() - 1) == '?') || input.charAt(input.length() - 1) == ',')) {
                    ServerLoggers.systemLogger.info(input.substring(0, input.length() - 2));
                    eventBus.fireValueChanged(SCANNER_SID, input.substring(0, input.length() - 2));
                    input = "";
                    e.consume();
                }
            } else if(recording) {
                input += e.getKeyChar();
                e.consume();
            }
        }
        return false;
    }

    private void install() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    private void uninstall() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    }

}
