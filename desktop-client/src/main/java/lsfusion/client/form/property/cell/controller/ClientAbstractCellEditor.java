package lsfusion.client.form.property.cell.controller;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.CellTableInterface;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.base.SwingUtils.computeAbsoluteLocation;

public class ClientAbstractCellEditor extends AbstractCellEditor implements PropertyTableCellEditor {
    private final JTable jTable;
    private final CellTableInterface table;
    private PropertyEditor propertyEditor;

    public ClientAbstractCellEditor(CellTableInterface table) {
        assert table instanceof JTable;

        this.jTable = (JTable) table;
        this.table = table;
    }

    public Component getTableCellEditorComponent(JTable itable, Object value, boolean selected, int row, int column) {
        ClientPropertyDraw property = table.getProperty(row, column);
        if (property == null) {
            //жто может быть в дереве
            return null;
        }

        propertyEditor = table.getCurrentEditType().getChangeEditorComponent(jTable, table.getForm(), property, table.getCurrentEditValue());
        propertyEditor.setTableEditor(this);

        assert propertyEditor != null;

        Component component = propertyEditor.getComponent(computeAbsoluteLocation(jTable), jTable.getCellRect(row, column, false), null);

        assert component != null;

        component.setFont(property.design.getFont(jTable));

        return component;
    }

    public JTable getTable() {
        return jTable;
    }

    public Object getCellEditorValue() {
        return propertyEditor.getCellEditorValue();
    }

    @Override
    public void stopCellEditingLater() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                stopCellEditing();
            }
        });
    }

    @Override
    public boolean stopCellEditing() {
        return propertyEditor.stopCellEditing() && super.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        propertyEditor.cancelCellEditing();
        super.cancelCellEditing();
    }
}
