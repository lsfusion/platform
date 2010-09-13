package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TableCellView extends JPanel implements CellView {

    private final JLabel label;
    private final CellTable table;

    private final ClientPropertyDraw key;
    private final ClientFormController form;

    int getID() {
        return key.getID();
    }

    @Override
    public int hashCode() {
        return getID();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TableCellView && ((TableCellView) o).getID() == this.getID();
    }

    public TableCellView(ClientPropertyDraw key, ClientFormController form) {

        setOpaque(false);

        this.key = key;
        this.form = form;

        setLayout(new BoxLayout(this, (key.panelLabelAbove ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS)));

        label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        label.setText(key.getFullCaption());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(label);

        table = new CellTable(key.readOnly) {

            protected boolean cellValueChanged(Object value) {
                return listener.cellValueChanged(value);
            }

            public boolean isDataChanging() {
                return true;
            }

            public ClientPropertyDraw getProperty(int col) {
                return TableCellView.this.key;
            }

            public ClientFormController getForm() {
                return TableCellView.this.form;
            }
        };

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isMetaDown() && e.getClickCount() >= 2) {
                    rightClick();
                }
            }
        });

        table.setBorder(BorderFactory.createLineBorder(Color.gray));

        table.keyChanged(key);

        add(table);

    }

    public void rightClick() {
        startEditing(null);
    }

    @Override
    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    @Override
    public void setFocusable(boolean focusable) {
        table.setFocusable(focusable);
        table.setCellSelectionEnabled(focusable);
    }

    public JComponent getComponent() {
        return this;
    }

    private CellViewListener listener;

    public void addListener(CellViewListener listener) {
        this.listener = listener;
    }

    public void setValue(Object ivalue) {
        table.setValue(ivalue);
    }

    public void startEditing(KeyEvent e) {

        if (table.isEditing()) return;

        table.editCellAt(0, 0);

        final Component tableEditor = table.getEditorComponent();
        if (tableEditor != null) {

            // устанавливаем следущий компонент фокуса на текущий
            if (tableEditor instanceof JComponent) {
                ((JComponent) tableEditor).setNextFocusableComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
            }

            tableEditor.requestFocusInWindow();
        }
    }
}
