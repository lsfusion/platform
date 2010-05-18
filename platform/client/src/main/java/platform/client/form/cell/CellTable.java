package platform.client.form.cell;

import platform.client.form.SingleCellTable;
import platform.client.form.ClientForm;
import platform.client.logics.ClientCellView;
import platform.base.BaseUtils;

import javax.swing.table.AbstractTableModel;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.*;

public abstract class CellTable extends SingleCellTable
                        implements ClientCellViewTable {

    private Object value;

    public CellTable() {
        super();

        setModel(new PropertyModel());

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

    }

    public void keyChanged(ClientCellView key) {

        setMinimumSize(key.getMinimumSize(this));
        setPreferredSize(key.getPreferredSize(this));
        setMaximumSize(key.getMaximumSize(this));
    }

    public void setValue(Object value) {
        this.value = value;
        repaint();
    }

    public void stopEditing() {

        CellEditor editor = getCellEditor();
        if (editor != null)
            editor.stopCellEditing();
   }

    class PropertyModel extends AbstractTableModel {

        public int getRowCount() {
            return 1;
        }

        public int getColumnCount() {
            return 1;
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public Object getValueAt(int row, int col) {
                return value;
        }

        public void setValueAt(Object value, int row, int col) {
            if (BaseUtils.nullEquals(value, getValueAt(row, col))) return;
            cellValueChanged(value);
        }

    }

    protected abstract boolean cellValueChanged(Object value);

    public abstract boolean isDataChanging();
    public abstract ClientCellView getCellView(int col);
    public abstract ClientForm getForm();

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        // пусть Enter обрабатывает верхний контейнер, если
        //noinspection SimplifiableIfStatement
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0 && pressed && !isDataChanging()) return false;

        return super.processKeyBinding(ks, e, condition, pressed);
    }
}
