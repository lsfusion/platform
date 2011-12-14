package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.ClientFormController;
import platform.client.form.SingleCellTable;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public abstract class CellTable extends SingleCellTable
        implements CellTableInterface {

    boolean checkEquals = true;
    private Object value;
    private boolean readOnly;

    ClientGroupObjectValue columnKey;

    public CellTable(boolean readOnly, ClientGroupObjectValue columnKey) {
        super();

        this.readOnly = readOnly;
        this.columnKey = columnKey;

        setModel(new PropertyModel());

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());
        setBorder(BorderFactory.createLineBorder(Color.gray));
    }

    public void keyChanged(ClientPropertyDraw key) {

        checkEquals = key.checkEquals;

        setMinimumSize(key.getMinimumSize(this));
        setPreferredSize(key.getPreferredSize(this));
        setMaximumSize(key.getMaximumSize(this));
    }

    public void setValue(Object value) {
        this.value = value;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        String tooltip;
        if (!BaseUtils.isRedundantString(value)) {
            tooltip = value.toString();
            if (value instanceof Date) {
                tooltip = Main.formatDate(value);
            }
            return SwingUtils.toMultilineHtml(BaseUtils.rtrim(tooltip), createToolTip().getFont());
        } else {
            return null;
        }
    }

    public abstract ClientPropertyDraw getProperty();

    public ClientPropertyDraw getProperty(int row, int column) {
        return getProperty();
    }

    public ClientGroupObjectValue getKey(int row, int col) {
        return columnKey;
    }

    public void writeSelectedValue(String value) {
        Object oValue;
        try {
            oValue = getProperty().parseString(getForm(), columnKey, value, isDataChanging());
        } catch (ParseException pe) {
            oValue = null;
        }
        if (oValue != null) {
            cellValueChanged(oValue, false);
        }
    }

    public void pasteTable(List<List<String>> table) {
        if (!table.isEmpty() && !table.get(0).isEmpty()) {
            writeSelectedValue(table.get(0).get(0));
        }
    }

    public void stopEditing() {
        CellEditor editor = getCellEditor();
        if (editor != null) {
            if (editor instanceof ClientAbstractCellEditor)
                ((ClientAbstractCellEditor) editor).hidePopupIfNotNull();
            editor.stopCellEditing();
        }
    }

    @Override
    public void removeEditor(){
        if (cellEditor != null && cellEditor instanceof ClientAbstractCellEditor)
            ((ClientAbstractCellEditor) cellEditor).hidePopupIfNotNull();
        super.removeEditor();
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
            cellValueChanged(value, true);
        }
    }

    protected abstract boolean cellValueChanged(Object value, boolean aggValue);

    public abstract boolean isDataChanging();

    public abstract ClientFormController getForm();

    public boolean isSelected(int row, int column) {
        return false;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        // пусть Enter обрабатывает верхний контейнер, если
        //noinspection SimplifiableIfStatement
        if (ks.equals(KeyStrokes.getEnter()) && !isDataChanging()) return false;
        return super.processKeyBinding(ks, e, condition, pressed);
    }
}
