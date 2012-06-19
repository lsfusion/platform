package platform.client.form.tree;

import javax.swing.*;
import javax.swing.text.Position;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * c/p from BasicTreeUI
 */
public class TreeGroupQuickSearchHandler extends KeyAdapter {
    public final long timeFactor = 1000L;

    private final TreeGroupTable treeTable;

    private String prefix = "";
    private String typedString = "";
    private long lastTime = 0L;

    public TreeGroupQuickSearchHandler(TreeGroupTable treeTable) {
        this.treeTable = treeTable;
    }

    /**
     * Invoked when a key has been typed.
     * <p/>
     * Moves the keyboard focus to the first element whose prefix matches the
     * sequence of alphanumeric keys pressed by the user with delay less
     * than value of <code>timeFactor</code> (1000 milliseconds).
     * Subsequent same key presses move the keyboard
     * focus to the next object that starts with the same letter until another
     * key is pressed, then it is treated as the prefix with appropriate number
     * of the same letters followed by first typed another letter.
     */
    public void keyTyped(KeyEvent e) {
        if (treeTable.getRowCount() > 0
            && treeTable.hasFocus()
            && treeTable.isEnabled()
            && treeTable.isHierarchical(treeTable.getSelectedColumn())
            && !e.isAltDown()
            && !isMenuShortcutKeyDown(e)
            && !isNavigationKey(e)) {

            boolean startingFromSelection = true;

            char c = e.getKeyChar();

            long time = e.getWhen();
            int startingRow = treeTable.getSelectedRow();
            if (time - lastTime < timeFactor) {
                typedString += c;
                if ((prefix.length() == 1) && (c == prefix.charAt(0))) {
                    // Subsequent same key presses move the keyboard focus to the next
                    // object that starts with the same letter.
                    startingRow++;
                } else {
                    prefix = typedString;
                }
            } else {
                startingRow++;
                typedString = "" + c;
                prefix = typedString;
            }
            lastTime = time;

            if (startingRow < 0 || startingRow >= treeTable.getRowCount()) {
                startingFromSelection = false;
                startingRow = 0;
            }
            TreePath path = treeTable.getNextMatch(prefix, startingRow, Position.Bias.Forward);
            if (path == null && startingFromSelection) {
                path = treeTable.getNextMatch(prefix, 0, Position.Bias.Forward);
            }

            if (path != null) {
                treeTable.setSelectionPath(path);
                treeTable.scrollPathToVisible(path);
            }
        }
    }

    /**
     * Invoked when a key has been pressed.
     * <p/>
     * Checks to see if the key event is a navigation key to prevent
     * dispatching these keys for the first letter navigation.
     */
    public void keyPressed(KeyEvent e) {
        if (isNavigationKey(e)) {
            prefix = "";
            typedString = "";
            lastTime = 0L;
        }
    }

    /**
     * Returns whether or not the supplied key event maps to a key that is used for
     * navigation.  This is used for optimizing key input by only passing non-
     * navigation keys to the first letter navigation mechanism.
     */
    private boolean isNavigationKey(KeyEvent event) {
        InputMap inputMap = treeTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke key = KeyStroke.getKeyStrokeForEvent(event);

        return inputMap != null && inputMap.get(key) != null;
    }

    static boolean isMenuShortcutKeyDown(InputEvent event) {
        return (event.getModifiers() &
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
    }
}