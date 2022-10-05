package lsfusion.client.base;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.lambda.ERunnable;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.property.table.view.TableTransferHandler;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.property.Compare;
import org.jdesktop.swingx.SwingXUtilities;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.client.base.view.SwingDefaults.getSingleCellTableIntercellSpacing;

public class SwingUtils {

    private static Map<String, Icon> icons = new HashMap<>();

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

        return comp == null ? MainFrame.instance : (Window) comp;
    }

    public static void assertDispatchThread() {
        Preconditions.checkState(EventQueue.isDispatchThread(), "should be executed in dispatch thread");
    }

    public static Point computeAbsoluteLocation(Component comp) {
        Point result = new Point(0, 0);
        SwingUtilities.convertPointToScreen(result, comp);
        return result;
    }

    public static Point translate(Point p, int dx, int dy) {
        Point np = new Point(p);
        np.translate(dx, dy);
        return np;
    }

    public static void invokeLater(final ERunnable runnable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    Throwables.propagate(t);
                }
            }
        });
    }

    public static void invokeAndWait(final ERunnable runnable) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    Throwables.propagate(t);
                }
            }
        });
    }

    private final static WeakHashMap<String, SingleActionTimer> timers = new WeakHashMap<>();

    public static void invokeLaterSingleAction(final String actionID, final ActionListener actionListener, int delay) {
        stopSingleAction(actionID, false);

        if (actionListener != null) {
            SingleActionTimer timer = SingleActionTimer.create(actionID, delay, actionListener);

            timers.put(actionID, timer);

            timer.start();
        }
    }

    public static void stopSingleAction(String actionID, boolean execute) {
        SingleActionTimer timer = timers.get(actionID);
        if (timer != null) {
            if (execute) {
                timer.forceExecute();
            } else {
                timer.cancel();
            }
        }
    }

    public static void commitDelayedGroupObjectChange(ClientGroupObject groupObject) {
        if (groupObject != null) {
            SwingUtils.stopSingleAction(groupObject.getActionID(), true);
        }
    }

    public static void cancelDelayedGroupObjectChange(ClientGroupObject groupObject) {
        if (groupObject != null) {
            SwingUtils.stopSingleAction(groupObject.getActionID(), false);
        }
    }

    public static final int YES_BUTTON = 0;
    public static final int NO_BUTTON = 1;

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, boolean cancel) {
        return showConfirmDialog(parentComponent, message, title, messageType, 0, cancel, 0);
    }
    
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, boolean cancel, int timeout, int initialValue) {
        return showConfirmDialog(parentComponent, message, title, messageType, initialValue, cancel, timeout);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, int initialValue,
                                        boolean cancel, int timeout) {

        Object[] options = {UIManager.getString("OptionPane.yesButtonText"),
                UIManager.getString("OptionPane.noButtonText")};
        if (cancel) {
            options = BaseUtils.add(options, UIManager.getString("OptionPane.cancelButtonText"));
        }

        JOptionPane dialogPane = new JOptionPane(getMessageTextPane(message),
                messageType,
                cancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION,
                null, options, options[initialValue]);

        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("RIGHT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("UP"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("LEFT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("DOWN"));

        final JDialog dialog = dialogPane.createDialog(parentComponent, title);
        if (timeout != 0) {
            final java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer.cancel();
                    dialog.setVisible(false);
                }
            }, timeout);
        }
        dialog.setVisible(true);

        if (dialogPane.getValue() == JOptionPane.UNINITIALIZED_VALUE)
            return initialValue;
        if (dialogPane.getValue() == options[0]) {
            return JOptionPane.YES_OPTION;
        } else {
            if (!cancel || dialogPane.getValue() == options[1])
                return JOptionPane.NO_OPTION;
            else
                return JOptionPane.CANCEL_OPTION;
        }
    }

    public static JTextPane getMessageTextPane(Object message) {
        JTextPane textPane = new JTextPane();
        textPane.setText(String.valueOf(message)); //message can be null
        textPane.setEditable(false);
        int width = (int) (MainFrame.instance.getRootPane().getWidth() * 0.3);
        textPane.setSize(new Dimension(width, 10));
        if(getWidth(String.valueOf(message)) >= width) { //set preferred size only for text with long lines
            int height = Math.min((int) (MainFrame.instance.getRootPane().getHeight() * 0.9), textPane.getPreferredSize().height);
            textPane.setPreferredSize((new Dimension(width, height)));
        }
        textPane.setBackground(null);
        return textPane;
    }

    private static int getWidth(String message) {
        try {
            FontMetrics metrics = MainFrame.instance.getRootPane().getGraphics().getFontMetrics();
            int maxWidth = 0;
            if (metrics != null) {
                for (String line : message.split("\n")) {
                    int width = metrics.stringWidth(line);
                    if (width > maxWidth)
                        maxWidth = width;
                }
            }
            return maxWidth;
        } catch (Exception e) {
            return 0;
        }
    }

    // приходится писать свой toString у KeyStroke, поскольку, по умолчанию, используется абсолютно кривой
    public static String getKeyStrokeCaption(KeyStroke changeKey) {
        return changeKey.toString().replaceAll("typed ", "").replaceAll("pressed ", "").replaceAll("released ", "");
    }

    // запрашивает положение объекта, чтобы он не вылезал за экран
    public static void requestLocation(Window window, Point onScreen) {
        Dimension screen = getUsableDeviceBounds();

        onScreen.x = max(10, min(onScreen.x, screen.width - window.getWidth() - 10));
        onScreen.y = max(10, min(onScreen.y, screen.height - window.getHeight() - 10));
        window.setLocation(onScreen);
    }

    public static Dimension clipDimension(Dimension toClip, Dimension min, Dimension max) {
        return new Dimension(max(min.width, min(max.width, toClip.width)),
                             max(min.height, min(max.height, toClip.height))
        );
    }

    /**
     * обрезает до размеров экрана минус 20 пикселей
     */
    public static Dimension clipToScreen(Dimension toClip) {
        Dimension screen = getUsableDeviceBounds();
        return clipDimension(toClip, new Dimension(0, 0), new Dimension(screen.width, screen.height));
    }

    /**
     * c/p from org.jdesktop.swingx.util.WindowUtils
     */
    public static Dimension getUsableDeviceBounds() {
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
        Class swingUtilities2Class = ReflectionUtils.classForName("sun.swing.SwingUtilities2");
        if(swingUtilities2Class != null) {
            //SwingUtilities2.getFontMetrics(null, font);
            FontMetrics fm = ReflectionUtils.getPrivateMethodValue(swingUtilities2Class, null, "getFontMetrics", new Class[] {JComponent.class, Font.class}, new Object[] {null, font});
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
        }
        return result += line + "</html>";
    }

    public static Icon getSystemIcon(String extension) {
        if (icons.containsKey(extension)) {
            return icons.get(extension);
        } else {
            Icon icon = null;
            File file = null;
            try {
                file = File.createTempFile("icon", "." + extension);
                FileSystemView view = FileSystemView.getFileSystemView();
                icon = view.getSystemIcon(file);
                icons.put(extension, icon);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(file != null && !file.delete()) {
                    file.deleteOnExit();
                }
            }
            return icon;
        }
    }

    public static ClientFormLayout getClientFormLayout(Component comp) {
        while (comp != null) {
            if (comp instanceof ClientFormLayout) {
                return (ClientFormLayout) comp;
            }
            comp = comp.getParent();
        }
        return null;
    }

    public static Window getActiveWindow() {
        return getSelectedWindow(Frame.getFrames());
    }

    public static Window getActiveVisibleWindow() {
        Container selectedWindow = getSelectedWindow(Frame.getFrames());
        while (selectedWindow != null && (!selectedWindow.isVisible())) {
            selectedWindow = selectedWindow.getParent();
        }
        return getWindow(selectedWindow);
    }

    private static Window getSelectedWindow(Window[] windows) {
        for (Window window : windows) {
            if (window.isActive()) {
                return window;
            } else {
                Window[] ownedWindows = window.getOwnedWindows();
                if (ownedWindows != null) {
                    Window selectedWindow = getSelectedWindow(ownedWindows);
                    if (selectedWindow != null) {
                        return selectedWindow;
                    }
                }
            }
        }
        return null;
    }

    /**
     * c/p from javax.swing.plaf.basic.BasicGraphicsUtils#isMenuShortcutKeyDown(java.awt.event.InputEvent)
     */
    public static boolean isMenuShortcutKeyDown(InputEvent event) {
        return (event.getModifiers() &
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
    }

    /**
     * c/p from JXTable.isFocusOwnerDescending
     */
    public static boolean isFocusOwnerDescending(Component component) {
        Component focusOwner = KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            return false;
        }
        if (SwingXUtilities.isDescendingFrom(focusOwner, component)) {
            return true;
        }
        Component permanent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        return SwingXUtilities.isDescendingFrom(permanent, component);
    }

    public static void setupClientTable(final JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSurrendersFocusOnKeystroke(false);
//        table.setSurrendersFocusOnKeystroke(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        table.getTableHeader().setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);

        if (table instanceof TableTransferHandler.TableInterface) {
            table.setTransferHandler(new TableTransferHandler((TableTransferHandler.TableInterface) table));
        }

        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (table.getEditorComponent() != null) {
                    table.getEditorComponent().requestFocusInWindow();
                }
            }
        });
    }

    public static void setupSingleCellTable(final JTable table) {
        table.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                table.changeSelection(0, 0, false, false);
            }

            public void focusLost(FocusEvent e) {
                table.getSelectionModel().clearSelection();
            }
        });

        // для таблиц с одной ячейкой будем сами мэнэджить перадачу фокуса,
        // это нужно потому, что setFocusTraversalKeys на самом деле используется и для дочерних компонентов,
        // т.е. в случае таблицы - для editorComp
        // из-за этого при использовании этих кнопок во время редактирования фокус переходит в таблицу без окончания редактирования
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<>());
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new HashSet<>());

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "forward-traversal");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getTab(), "forward-traversal");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getCtrlTab(), "forward-traversal");

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getShiftTab(), "backward-traversal");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getCtrlShiftTab(), "backward-traversal");

        table.getActionMap().put("forward-traversal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                    return;
                }
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(table);
            }
        });
        table.getActionMap().put("backward-traversal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                    return;
                }
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(table);
            }
        });

        table.setIntercellSpacing(new Dimension(getSingleCellTableIntercellSpacing(), getSingleCellTableIntercellSpacing()));

        table.setBorder(SwingDefaults.getTextFieldBorder());
    }

    public static Border randomBorder() {
        int r = (int) (Math.random() * 256);
        int g = (int) (Math.random() * 256);
        int b = (int) (Math.random() * 256);
        return BorderFactory.createLineBorder(new Color(r, g, b), 2);
    }

    public static boolean isRecursivelyVisible(Component component) {
        return component.isVisible() && (component.getParent() == null || isRecursivelyVisible(component.getParent()));
    }

    private static final class SingleActionTimer extends Timer {
        private boolean stopped = false;

        public SingleActionTimer(int delay, final ActionListener actionListener) {
            super(delay, actionListener);
            setRepeats(false);
        }

        public void cancel() {
            stopped = true;
            stop();
        }

        public void forceExecute() {
            assert getActionListeners().length == 1;
            getActionListeners()[0].actionPerformed(null);
        }

        public static SingleActionTimer create(final String actionID, int delay, final ActionListener actionListener) {
            final SingleActionTimer[] timerHolder = new SingleActionTimer[1];
            ActionListener timerListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!timerHolder[0].stopped) {
                        actionListener.actionPerformed(e);
                        timerHolder[0].cancel();
                    }
                    timers.remove(actionID);
                }
            };

            SingleActionTimer timer = new SingleActionTimer(delay, timerListener);
            timerHolder[0] = timer;
            return timer;
        }
    }

    public static Rectangle getNewBoundsIfNotAlmostEquals(Component comp, int x, int y, int width, int height) {
        Rectangle rect = comp.getBounds();

        rect.x = changeIfNotAlmostEquals(rect.x, x);
        rect.y = changeIfNotAlmostEquals(rect.y, y);
        rect.width = changeIfNotAlmostEquals(rect.width, width);
        rect.height = changeIfNotAlmostEquals(rect.height, height);

        return rect;
    }

    private static int changeIfNotAlmostEquals(int currVal, int newVal) {
        return almostEquals(currVal, newVal) ? currVal : newVal;
    }

    public static boolean almostEquals(int a, int b) {
        return Math.abs(a - b) < 3;
    }

    public static void paintRightBottomCornerTriangle(Graphics2D graphics, int triangleSize, Color color, int x, int y, int w, int h) {
        paintCornerTriangle(graphics, triangleSize, color, x, y, w, h, false, false);
    }
    
    public static void paintCornerTriangle(Graphics2D graphics, int triangleSize, Color color, int x, int y, int w, int h, boolean left, boolean top) {
        int compRight = x + w;
        int compBottom = y + h;
        
        int[] xs;
        int[] ys;

        if (left) {
            xs = new int[]{x + triangleSize, x, x};
            ys = top ? new int[]{y, y, y + triangleSize} : new int[]{compBottom, compBottom, compBottom - triangleSize};
        } else {
            xs = new int[]{compRight, compRight, compRight - triangleSize};
            ys = top ? new int[]{y, y + triangleSize, y} : new int[]{compBottom - triangleSize, compBottom, compBottom};
        }

        Polygon polygon = new Polygon(xs, ys, 3);

        graphics.setColor(color);
        graphics.fillPolygon(polygon);
    }

    public static void drawHorizontalLine(Graphics2D graphics, Color color, int x1, int x2, int y) {
        drawLine(graphics, color, x1, y, x2, y);
    }

    public static void drawLine(Graphics2D graphics, Color color, int x1, int y1, int x2, int y2) {
        graphics.setColor(color);
        graphics.drawLine(x1, y1, x2, y2);
    }

    private static Class<?> tooltipListenerClass;
    private static KeyStroke closeTooltipKeyStroke;
    
    private static boolean tryToInitTooltipStuff() {
        if (tooltipListenerClass == null || closeTooltipKeyStroke == null) {
            for (Class<?> declaredClass : ToolTipManager.class.getDeclaredClasses()) {
                if (declaredClass.getCanonicalName().equals("javax.swing.ToolTipManager.AccessibilityKeyListener")) {
                    tooltipListenerClass = declaredClass;
                    break;
                }
            }

            try {
                Field hideTip = ToolTipManager.class.getDeclaredField("hideTip");
                if (hideTip != null) {
                    hideTip.setAccessible(true);
                    closeTooltipKeyStroke = (KeyStroke) hideTip.get(ToolTipManager.sharedInstance());
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}    
        }
        return tooltipListenerClass != null && closeTooltipKeyStroke != null;
    }
    
    /**
     * Все компоненты, для которых показывается всплывающая подсказка, регистрируются в <code>ToolTipManager</code> 
     * ({@link javax.swing.ToolTipManager#registerComponent(JComponent)}), который добавляет им свои <code>MouseMotionListener</code> 
     * и <code>KeyListener</code>. <code>KeyListener</code> оповещается глобально каждым компонентом, у которого он есть, 
     * при возникновении любого события клавиатуры в {@link Component#processKeyEvent(KeyEvent)}.
     * <p>
     * На время обработки нажатия клавиши Escape отключаем этот listener для некоторых focusable компонентов формы. Делалось, 
     * чтобы не нажимать Escape дважды для закрытия модальной формы, а также отмены редактирования дат и закрытия панели отбора
     * при показанной подсказке. 
     */
    public static void getAroundTooltipListener(JComponent component, KeyEvent event, Runnable process) {
        boolean init = tryToInitTooltipStuff();
        
        if (init && closeTooltipKeyStroke.equals(KeyStroke.getKeyStrokeForEvent(event))) {
            KeyListener tooltipListener = null;

            KeyListener[] keyListeners = component.getKeyListeners();
            for (KeyListener keyListener : keyListeners) {
                if (tooltipListenerClass.isAssignableFrom(keyListener.getClass())) {
                    tooltipListener = keyListener;
                    component.removeKeyListener(keyListener);
                }
            }

            process.run();

            if (tooltipListener != null) {
                component.addKeyListener(tooltipListener);
            }
        } else {
            process.run();
        }
    }

    // возвращает новую flexWidth
    private static double reducePrefsToBase(double prevFlexWidth, int column, double[] prefs, double[] flexes, int[] basePrefs) {
        double reduce = prefs[column] - basePrefs[column];
        assert greaterEquals(reduce, 0.0);
        if (equals(reduce, 0.0))
            return prevFlexWidth;

        double newFlexWidth = prevFlexWidth + reduce;
        double newTotalFlexes = 0.0;
        double prevTotalFlexes = 0.0;
        for (int i = 0; i < prefs.length; i++) {
            if (i != column) {
                double prevFlex = flexes[i];
                double newFlex = prevFlex * prevFlexWidth / newFlexWidth;
                flexes[i] = newFlex;
                newTotalFlexes += newFlex;
                prevTotalFlexes += prevFlex;
            }
        }
        assert greaterEquals(prevTotalFlexes, newTotalFlexes);
        flexes[column] += prevTotalFlexes - newTotalFlexes;
        prefs[column] = basePrefs[column];
        return newFlexWidth;
    }

    private static boolean greater(double a, double b) {
        return a - b > 0.001;
    }
    private static boolean greaterEquals(double a, double b) {
        return a - b > -0.001;
    }
    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < 0.001;
    }

    // viewfixed if view is fixed we can convert flex to pref, otherwise we can't
    public static double calculateNewFlexes(int column, double delta, int viewWidth, double[] prefs, double[] flexes, int[] basePrefs, double[] baseFlexes, boolean[] flexPrefs, boolean noParentFlex, Boolean resizeOverflow, int margins) {

        // ищем первую динамическую компоненту слева (она должна получить +delta, соответственно правая часть -delta)
        // тут есть варианты -delta идет одной правой колонке, или всем правых колонок, но так как
        // a) так как выравнивание по умолчанию левое, интуитивно при перемещении изменяют именно размер левой колонки, б) так как есть де-факто ограничение Preferred, вероятность получить нужный размер уменьшая все колонки куда выше
        // будем распределять между всеми правыми колонками

        int flexColumn = column;
        while (flexColumn >= 0 && baseFlexes[flexColumn] == 0)
            flexColumn--;

        // считаем общий текущий preferred
        double totalPref = 0;
        for (double pref : prefs) {
            totalPref += pref;
        }

        if (flexColumn < 0) {
            double restDelta = 0.0;
            if(resizeOverflow != null && !resizeOverflow) { // we shouldn't exceed viewWidth, but only if it is set explicitly (otherwise behaviour is pretty normal)
                double restWidth = viewWidth - totalPref - margins;
                if(delta > restWidth) {
                    restDelta = delta - restWidth;
                    delta = restWidth;
                }
            }

            return restDelta + (-reducePrefs(-delta, column, prefs, basePrefs, flexPrefs));
        }
        column = flexColumn;

        // сначала списываем delta справа налево pref (но не меньше basePref), ПОКА сумма pref > viewWidth !!! ( то есть flex не работает, работает ширина контейнера или minTableWidth в таблице)
        // тут можно было бы если идет расширение - delta > 0.0, viewWidth приравнять totalPref (соответственно запретить adjust, то есть pref'ы остались такими же) и reduce'ить остальные, но это пойдет в разрез с уменьшением (когда нужно уменьшать pref'ы иначе в исходное состояние не вернешься), поэтому логичнее исходить из концепции когда если есть scroll тогда просто расширяем колонки, если нет scroll'а пытаемся уместить все без скролла
        double exceedPrefWidth = totalPref - viewWidth;
        if (greaterEquals(exceedPrefWidth, 0.0)) {
            double prefReduceDelta = Math.min(-delta, exceedPrefWidth);
            delta += prefReduceDelta;
            reducePrefs(prefReduceDelta, column, prefs, basePrefs, null);

            assert greaterEquals(0.0, delta);

            exceedPrefWidth = 0;
        }

        if (equals(delta, 0.0)) // все расписали
            return delta;

        double flexWidth = -exceedPrefWidth;
        assert greaterEquals(flexWidth, 0.0);

        // можно переходить на basePref - flex (с учетом того что viewWidth может измениться, pref'ы могут быть как равны viewWidth в результате предыдущего шага, так и меньше)
        for (int i = 0; i < prefs.length; i++)
            flexWidth = reducePrefsToBase(flexWidth, i, prefs, flexes, basePrefs);

        //если flexWidth все еще равно 0 - вываливаемся (так как нельзя меньше preferred опускаться)
        if (equals(flexWidth, 0.0))
            return delta; // or maybe 0.0

        // запускаем изменение flex'а (пропорциональное)
        double totalFlex = 0;
        double totalRightFlexes = 0.0;
        double totalRightBaseFlexes = 0.0;
        for (int i = 0; i < flexes.length; i++) {
            double flex = flexes[i];
            double baseFlex = baseFlexes[i];
            if (i > column) {
                totalRightFlexes += flex;
                totalRightBaseFlexes += baseFlex;
            }
            totalFlex += flex;
        }

        // flex колонки увеличиваем на нужную величину, соответственно остальные flex'ы надо уменьшить на эту величину
        double toAddFlex = delta * totalFlex / flexWidth;
        if (greater(0.0, toAddFlex + flexes[column])) // не shrink'аем, но и левые столбцы не уменьшаются (то есть removeLeftFlex false)
            toAddFlex = -flexes[column];

        double restFlex = 0.0; // flex that wasn't added to the right flexes
        double toAddRightFlex = toAddFlex;
        if(equals(totalRightBaseFlexes, 0.0)) { // if there are no right flex columns, we don't change flexes
            restFlex = toAddRightFlex;
        } else {
            if (toAddRightFlex > totalRightFlexes) { // we don't want to have negative flexes
                restFlex = toAddRightFlex - totalRightFlexes;
                toAddRightFlex = totalRightFlexes;
            }
            for (int i = column + 1; i < flexes.length; i++) {
                if (greater(totalRightFlexes, 0.0))
                    flexes[i] -= flexes[i] * toAddRightFlex / totalRightFlexes;
                else {
                    assert equals(flexes[i], 0.0);
                    flexes[i] = -baseFlexes[i] * toAddRightFlex / totalRightBaseFlexes;
                }
            }
        }

        flexes[column] += toAddFlex - restFlex;

        // если и так осталась, то придется давать preferred (соответственно flex не имеет смысла) и "здравствуй" scroll
        if (!equals(restFlex, 0.0) && noParentFlex && (resizeOverflow != null ? resizeOverflow : true)) {
            // we can't increase / decrease right part using flexes (we're out of it they are zero already, since restflex is not zero), so we have to use prefs instead
            // assert that right flexes are zero (so moving flex width to prefs in left part won't change anything)
            for (int i = 0; i < column; i++)
                prefs[i] += flexWidth * flexes[i] / totalFlex;
            prefs[column] += flexWidth * ((flexes[column] + restFlex) / totalFlex);
            restFlex = 0.0;
        }
        return restFlex * flexWidth / totalFlex;
    }

    private static double reducePrefs(double delta, int column, double[] prefs, int[] basePrefs, boolean[] filterColumns) {
        for (int i = column; i >= 0; i--) {
            if(filterColumns == null || filterColumns[i]) {
                double maxReduce = prefs[i] - basePrefs[i];
                double reduce = Math.min(delta, maxReduce);
                prefs[i] -= reduce;
                delta -= reduce;
                if (equals(delta, 0.0))
                    break;
            }
        }
        return delta;
    }

    private static void adjustFlexesToFixedTableLayout(int viewWidth, double[] prefs, boolean[] flexes, double[] flexValues) {
        double minRatio = Double.MAX_VALUE;
        double totalPref = 0;
        double totalFlexValues = 0.0;
        for(int i=0;i<prefs.length;i++) {
            if(flexes[i]) {
                double ratio = flexValues[i] / prefs[i];
                minRatio = Math.min(minRatio, ratio);
                totalFlexValues += flexValues[i];
            }
            totalPref += prefs[i];
        }
        double flexWidth = Math.max((double)viewWidth - totalPref, 0.0);
        for(int i=0;i<prefs.length;i++) {
            if(flexes[i])
                prefs[i] = (prefs[i] + flexWidth * flexValues[i] / totalFlexValues) / (1.0 + flexWidth * minRatio / totalFlexValues);
        }
    }

    // изменяется prefs
    public static void calculateNewFlexesForFixedTableLayout(int column, int delta, int viewWidth, double[] prefs, int[] basePrefs, boolean[] flexes, Boolean resizeOverflow) {
        double[] flexValues = new double[prefs.length];
        double[] baseFlexValues = new double[prefs.length];
        boolean[] flexPrefs = new boolean[prefs.length];
        for(int i=0;i<prefs.length;i++) {
            if(flexes[i]) {
                flexValues[i] = prefs[i];
                baseFlexValues[i] = basePrefs[i];
            } else {
                flexValues[i] = 0.0;
                baseFlexValues[i] = 0.0;
            }
            flexPrefs[i] = false;
        }

        calculateNewFlexes(column, delta, viewWidth, prefs, flexValues, basePrefs, baseFlexValues, flexPrefs, true, resizeOverflow, 0);

        adjustFlexesToFixedTableLayout(viewWidth, prefs, flexes, flexValues);
    }

    private static String showIfVisible = "showIfVisible";
    public static void setShowIfVisible(JComponent component, boolean visible) {
        component.putClientProperty(showIfVisible, String.valueOf(visible));
        updateVisibility(component);
    }

    private static String gridVisible = "gridVisible";
    public static void setGridVisible(JComponent component, boolean visible) {
        component.putClientProperty(gridVisible, String.valueOf(visible));
        updateVisibility(component);
    }

    private static void updateVisibility(JComponent component) {
        component.setVisible(isVisible(component, showIfVisible) && isVisible(component, gridVisible));
    }

    private static boolean isVisible(JComponent component, String key) {
        Object value = component.getClientProperty(key);
        return isRedundantString(value) || Boolean.parseBoolean((String) value);
    }

    public static Object escapeSeparator(Object value, Compare compare) {
        return value instanceof String && compare != null && compare.escapeComma() ? ((String) value).replace(MainController.matchSearchSeparator, "\\" + MainController.matchSearchSeparator) : value;
    }
}
