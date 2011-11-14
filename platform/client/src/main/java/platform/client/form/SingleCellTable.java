package platform.client.form;

import platform.client.SwingUtils;
import platform.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

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
                                        KeyStrokes.getForwardTraversalKeyStroke());

        SwingUtils.addFocusTraversalKey(this,
                                        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                        KeyStrokes.getBackwardTraversalKeyStroke());
        getColumnModel().setColumnMargin(2);
        setRowMargin(2);
    }

    // приходится делать вот таким извращенным способом, поскольку ComponentListener срабатывает после перерисовки формы
    @Override
    public void setBounds(int x, int y, int width, int height) {
        rowHeight = height;
        super.setBounds(x, y, width, height);
    }

     @Override
     protected boolean isEditOnSingleClick(int row, int column){
        return false;
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        // сами обрабатываем нажатие клавиши Enter
        if (ks.equals(KeyStrokes.getEnter())) {
            if (isEditing()) {
                Component editorComp = getEditorComponent(), nextComp = null;
                if (editorComp instanceof JComponent) {
                    //noinspection deprecation
                    nextComp = ((JComponent) editorComp).getNextFocusableComponent();
                }

                getCellEditor().stopCellEditing();

                // приходится таким волшебным образом извращаться, поскольку в stopCellEditing в явную устанавливается фокус на JTable
                // это не устраивает, если по нажатии кнопки из другого компонена вызывается редактирование, а потом необходимо вернуть фокус обратно
                if (nextComp != null) {
                    nextComp.requestFocusInWindow();
                }
            } else {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
            }
            return true;
        }

        return super.processKeyBinding(ks, e, condition, pressed);
    }
}
