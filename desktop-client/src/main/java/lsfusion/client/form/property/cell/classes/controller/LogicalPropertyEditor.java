package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

public class LogicalPropertyEditor extends JCheckBox implements PropertyEditor {
    private boolean newValue;
    private PropertyTableCellEditor tableEditor;

    public LogicalPropertyEditor(Object value) {
        setHorizontalAlignment(JCheckBox.CENTER);

        setOpaque(true);
        setBackground(Color.white);

        // set new value because we'll finish editing immediately
        newValue = value == null;
        model.setSelected(newValue);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tableEditor.stopCellEditingLater();
            }
        });
    }

    @Override
    public void setSelected(boolean b) {
        //будем менять модель сами, чтобы точно контролировать состояние чекбокса
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    public Object getCellEditorValue() {
        return newValue ? true : null;
    }

    @Override
    public boolean stopCellEditing(){
        return true;
    }
}
