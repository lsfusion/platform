package lsfusion.client.form.property.cell.controller;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.AsyncChangeCellTableInterface;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.base.SwingUtils.computeAbsoluteLocation;

public class ClientAbstractCellEditor extends AbstractCellEditor implements PropertyTableCellEditor {
    private final JTable jTable;
    private final AsyncChangeCellTableInterface asyncTable;
    private PropertyEditor propertyEditor;

    public ClientAbstractCellEditor(AsyncChangeCellTableInterface asyncTable) {
        assert asyncTable instanceof JTable;

        this.jTable = (JTable) asyncTable;
        this.asyncTable = asyncTable;
    }

    public Component getTableCellEditorComponent(JTable itable, Object value, boolean selected, int row, int column) {
        ClientPropertyDraw property = asyncTable.getProperty(row, column);
        if (property == null) {
            //жто может быть в дереве
            return null;
        }

        propertyEditor = asyncTable.getCurrentEditType().getChangeEditorComponent(jTable, asyncTable.getForm(), property, asyncTable, asyncTable.getCurrentEditValue());
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
