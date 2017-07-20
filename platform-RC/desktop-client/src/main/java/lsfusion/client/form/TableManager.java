package lsfusion.client.form;

import javax.swing.*;

public class TableManager {
    private JTable currentTable = null;

    public void setCurrentEditingTable(JTable currentTable) {
        this.currentTable = currentTable;
    }

    public JTable getCurrentTable() {
        return currentTable;
    }

    public boolean isEditing() {
        return currentTable != null && currentTable.isEditing();
    }

    public boolean commitCurrentEditing() {
        return commitCurrentEditing(false);
    }

    public void commitOrCancelCurrentEditing() {
        commitCurrentEditing(true);
    }

    private boolean commitCurrentEditing(boolean cancelIfCantStop) {
        if (currentTable != null && currentTable.isEditing()) {
            boolean editingStopped = currentTable.getCellEditor().stopCellEditing();
            if (!editingStopped && cancelIfCantStop) {
                currentTable.getCellEditor().cancelCellEditing();
            }

            return editingStopped;
        }
        return true;
    }

}
