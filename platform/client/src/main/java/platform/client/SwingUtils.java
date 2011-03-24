package platform.client;

import sun.swing.SwingUtilities2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

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
                removeFocusable((Container) comp);
            }
        }
    }

    public static Window getWindow(Component comp) {

        while (comp != null && !(comp instanceof Window)) {
            comp = comp.getParent();
        }

        return (Window) comp;
    }

    public static JTable getJTable(Component comp) {

        while (comp != null && !(comp instanceof JTable)) {
            comp = comp.getParent();
        }

        return (JTable) comp;
    }

    public static void commitEditing(JTable table) {

        if (table.isEditing() && table.getCellEditor() != null) {
            if (!table.getCellEditor().stopCellEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
        }
    }

    public static void commitCurrentEditing() {

        Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JTable table = getJTable(comp);
        if (table != null) {
            commitEditing(table);
        }
    }

    public static Point computeAbsoluteLocation(Component comp) {
        Point result = new Point(0, 0);
        SwingUtilities.convertPointToScreen(result, comp);
        return result;
    }

    private final static WeakHashMap<String, Timer> timers = new WeakHashMap<String, Timer>();

    public static void invokeLaterSingleAction(final String actionID, final ActionListener actionListener, int delay) {

        stopSingleAction(actionID, false);

        if (actionListener != null) {

            final Timer timer = new Timer(delay, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    actionListener.actionPerformed(e);
                    timers.remove(actionID);
                }
            });
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
                for (ActionListener action : actions) {
                    action.actionPerformed(null);
                }
            }
            timer.stop();
            timers.remove(actionID);
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

        if (dialogPane.getValue() == options[0]) {
            return JOptionPane.YES_OPTION;
        } else {
            return JOptionPane.NO_OPTION;
        }
    }

    // приходится писать свой toString у KeyStroke, поскольку, по умолчанию, используется абсолютно кривой
    public static String getKeyStrokeCaption(KeyStroke editKey) {
        return editKey.toString().replaceAll("typed ", "").replaceAll("pressed ", "").replaceAll("released ", "");
    }

    // запрашивает положение объекта, чтобы он не вылезал за экран
    public static void requestLocation(Component comp, Point point) {
        Container parent = comp.getParent();
        point.x = Math.max(Math.min(point.x, parent.getX() + parent.getWidth() - comp.getWidth() - 20), 0);
        point.y = Math.max(Math.min(point.y, parent.getY() + parent.getHeight() - comp.getHeight() - 20), 0);
        comp.setLocation(point);
    }

    public static Dimension clipDimension(Dimension toClip, Dimension min, Dimension max) {
        return new Dimension(Math.max(min.width, Math.min(max.width, toClip.width)),
                             Math.max(min.height, Math.min(max.height, toClip.height))
        );
    }

    public static Dimension clipToScreen(Dimension toClip) {
        return clipDimension(toClip, new Dimension(0, 0), getUsableDeviceBounds());
    }

    /**
     * c/p from org.jdesktop.swingx.util.WindowUtils
     */
    private static Dimension getUsableDeviceBounds() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();

        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle bounds = gc.getBounds();
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);

        return new Dimension(bounds.width, bounds.height);
    }

    public static String toMultilineHtml(String text, Font font) {
        String result = "<html>";
        String line = "";
        FontMetrics fm = SwingUtilities2.getFontMetrics(null, font);
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width - 10;
        String delims = " \n";
        StringTokenizer st = new StringTokenizer(text, delims, true);
        String wordDelim = "";
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (delims.contains(token)) {
                if (token.equals("\n")) {
                    result += line;
                    line = "<br>" + wordDelim;
                    wordDelim = "";
                } else {
                    wordDelim += token;
                }
            } else {
                if (fm.stringWidth(line + wordDelim + token) >= screenWidth) {
                    result += line;
                    result += !line.equals("") ? "<br>" : "";
                    line = "";
                }
                line += wordDelim + token;
                wordDelim = "";
            }
        }
        return result += line + "</html>";
    }
}
