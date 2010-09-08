package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.SingleCellTable;
import platform.client.logics.ClientCell;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.KeyEvent;
import java.text.ParseException;

public abstract class CellTable extends SingleCellTable
        implements CellTableInterface {

    boolean checkEquals = true;
    private Object value;
    private boolean readOnly;

    public CellTable(boolean readOnly) {
        super();

        this.readOnly = readOnly;

        setModel(new PropertyModel());

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());
    }

    public void keyChanged(ClientCell key) {

        checkEquals = key.checkEquals();

        setMinimumSize(key.getMinimumSize(this));
        setPreferredSize(key.getPreferredSize(this));
        setMaximumSize(key.getMaximumSize(this));
    }

    public void setValue(Object value) {
        this.value = value;
        repaint();
    }

    @Override
    public Object convertValueFromString(String value, int row, int column) {
        Object parsedValue = null;
        try {
            parsedValue = getCell(column).parseString(getForm(), value);
        } catch (ParseException pe) {
            return null;
        }

        return parsedValue;
    }

    public void stopEditing() {

        CellEditor editor = getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    class PropertyModel extends AbstractTableModel {

        public int getRowCount() {
            return 1;
        }

        public int getColumnCount() {
            return 1;
        }

        public boolean isCellEditable(int row, int col) {
            return !readOnly;
        }

        public Object getValueAt(int row, int col) {
            return value;
        }

        public void setValueAt(Object value, int row, int col) {
            if (checkEquals && BaseUtils.nullEquals(value, getValueAt(row, col))) return;
            cellValueChanged(value);
        }
    }

    protected abstract boolean cellValueChanged(Object value);

    public abstract boolean isDataChanging();

    public abstract ClientCell getCell(int col);

    public abstract ClientFormController getForm();

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        // пусть Enter обрабатывает верхний контейнер, если
        //noinspection SimplifiableIfStatement
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() == 0 && pressed && !isDataChanging()) return false;

        return super.processKeyBinding(ks, e, condition, pressed);
    }
}
