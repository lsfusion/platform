package platform.client.form.tree;

import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class TreeTableTransferHandler extends TransferHandler {
    private ClientFormTreeTable table;

    public TreeTableTransferHandler(ClientFormTreeTable handleTable) {
        this.table = handleTable;
    }

    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            int row = table.getSelectionModel().getLeadSelectionIndex();
            int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

            if (row < 0 || row >= this.table.getRowCount() || column < 0 || column >= this.table.getColumnCount()) return null;

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
        if (c == table) {
            for (DataFlavor flavor : t.getTransferDataFlavors()) {
                if (String.class.isAssignableFrom(flavor.getRepresentationClass())) {
                    String value = null;
                    try {
                        value = (String) t.getTransferData(flavor);
                    } catch (Exception ignored) {
                    }
                    if (value != null) {
                        int row = table.getSelectionModel().getLeadSelectionIndex();
                        int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

                        Object oValue = table.convertValueFromString(value, row, column);
                        if (oValue != null) {
                            table.setValueAt(oValue, row, column);
                        }

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
