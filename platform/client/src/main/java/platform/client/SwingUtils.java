package platform.client;

import platform.client.form.ClientForm;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.ActionMapUIResource;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.*;

public class SwingUtils {

    public static void addFocusTraversalKey(Component comp, int id, KeyStroke key) {

        Set keys = comp.getFocusTraversalKeys(id);
        Set newKeys = new HashSet(keys);
        newKeys.add(key);
        comp.setFocusTraversalKeys(id, newKeys);
    }

    public static void removeFocusable(Container container) {

        container.setFocusable(false);
        for (Component comp : container.getComponents()) {
            comp.setFocusable(false);
            if (comp instanceof Container) {
                removeFocusable((Container)comp);
            }
        }
    }

    public static Window getWindow(Component comp) {

        while (comp != null && !(comp instanceof Window) ) comp = comp.getParent();

        return (Window)comp;
    }

    public static Point computeAbsoluteLocation(Component comp) {

        Point pt = new Point();
        while (comp != null ) {
            Point ptc = comp.getLocation();
            pt.x += ptc.x;
            pt.y += ptc.y;
            comp = comp.getParent();
        }

        return pt;
    }

    private final static Map<String, Timer> timers = new HashMap();
    public static void invokeLaterSingleAction(String actionID, ActionListener actionListener, int delay) {

        stopSingleAction(actionID, false);

        if (actionListener != null) {

            Timer timer = new Timer(delay, actionListener);
            timer.setRepeats(false);

            timer.start();

            timers.put(actionID, timer);
        }
    }

    public static void stopSingleAction(String actionID, boolean execute) {

        Timer timer = timers.get(actionID);
        if (timer != null && timer.isRunning()) {
            if (execute) {
                ActionListener[] actions = timer.getActionListeners();
                for (ActionListener action : actions)
                    action.actionPerformed(null);
            }
            timer.stop();
        }
    }

    public static final int YES_BUTTON = 0;
    public static final int NO_BUTTON = 1;

    public static int showConfirmDialog(JComponent parentComponent, Object message, String title, int messageType, int initialValue) {

        Object[] options = {UIManager.getString("OptionPane.yesButtonText"),
                            UIManager.getString("OptionPane.noButtonText")};

        JOptionPane dialogPane = new JOptionPane(message,
                                                 messageType,
                                                 JOptionPane.YES_NO_OPTION,
                                                 null, options, options[initialValue]);

        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("RIGHT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("UP"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("LEFT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("DOWN"));

        JDialog dialog = dialogPane.createDialog(parentComponent, title);
        dialog.setVisible(true);

        if (dialogPane.getValue() == options[0])
            return JOptionPane.YES_OPTION;
        else
            return JOptionPane.NO_OPTION;
    }

    // приходится писать свой toString у KeyStroke, поскольку, по умолчанию, используется абсолютно кривой
    public static String getKeyStrokeCaption(KeyStroke editKey) {
        return editKey.toString().replaceAll("typed ", "").replaceAll("pressed ", "").replaceAll("released ", "");
    }
}
