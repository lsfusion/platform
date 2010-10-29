package platform.client.form;

import platform.client.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class SingleCellTable extends ClientFormTable {

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

    // приходится делать вот таким извращенным способом, поскольку ComponentListener срабатывает после перерисовки формы
    @Override
    public void setBounds(int x, int y, int width, int height) {
        rowHeight = height;
        super.setBounds(x, y, width, height);
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
					int condition, boolean pressed) {

        // сами обрабатываем нажатие клавиши Enter
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0 && pressed) {

            if (isEditing()) {

                Component editorComp = getEditorComponent(), nextComp = null;
                if (editorComp instanceof JComponent)
                    nextComp = ((JComponent)editorComp).getNextFocusableComponent();

                getCellEditor().stopCellEditing();

                // приходится таким волшебным образом извращаться, поскольку в stopCellEditing в явную устанавливается фокус на JTable
                // это не устраивает, если по нажатии кнопки из другого компонена вызывается редактирование, а потом необходимо вернуть фокус обратно
                if (nextComp != null)
                    nextComp.requestFocusInWindow();
                
            } else
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();

            return true;

        } else
            return super.processKeyBinding(ks, e, condition, pressed);

    }

}
