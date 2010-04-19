package platform.client.form;

import platform.client.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class SingleCellTable extends ClientFormTable {

    protected SingleCellTable() {
        super();

        addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                requestFocusInWindow();
                changeSelection(0, 0, false, false);
            }

            public void focusLost(FocusEvent e) {
                getSelectionModel().clearSelection();
            }

        });

        SwingUtils.addFocusTraversalKey(this,
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));

/*        SwingUtils.addFocusTraversalKey(this,
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));*/

        SwingUtils.addFocusTraversalKey(this,
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));

   }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
					int condition, boolean pressed) {

        // сами обрабатываем нажатие клавиши Enter
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0 && pressed) {
            if (isEditing()) getCellEditor().stopCellEditing();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
            return true;
        } else
            return super.processKeyBinding(ks, e, condition, pressed);

    }

}
