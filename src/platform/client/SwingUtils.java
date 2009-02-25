package platform.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

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

    public final static Map<String, Timer> timers = new HashMap();
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

}
