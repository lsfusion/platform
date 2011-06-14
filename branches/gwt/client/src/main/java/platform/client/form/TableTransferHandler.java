package platform.client.form;

import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public abstract class TableTransferHandler extends TransferHandler {

    public interface TableInterface {
        int getRowCount();
        int getColumnCount();

        void writeSelectedValue(String value); // записать значение в текущую ячейку
    }
    protected abstract TableInterface getTable();

    // ghgfdffddf

    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            int row = table.getSelectionModel().getLeadSelectionIndex();
            int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

            if (row < 0 || row >= getTable().getRowCount() || column < 0 || column >= getTable().getColumnCount()) return null;

            Object value = table.getValueAt(row, column);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                value = BaseUtils.rtrim((String) value);
            }
            return new StringSelection(value.toString());
        }

        return null;
    }

    @Override
    public boolean importData(JComponent c, Transferable t) {
        TableInterface table = getTable();
        if (c == table) {
            for (DataFlavor flavor : t.getTransferDataFlavors()) {
                if (String.class.isAssignableFrom(flavor.getRepresentationClass())) {
                    String value = null;
                    try {
                        value = (String) t.getTransferData(flavor);
                    } catch (Exception ignored) {
                    }
                    if (value != null) {
                        table.writeSelectedValue(value);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getSourceActions(JComponent c) {
        return COPY;
    }
}
