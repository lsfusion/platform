package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

public class LogicalPropertyEditor extends JCheckBox implements PropertyEditor {
    private Boolean nextValue;
    private boolean threeState;
    private PropertyTableCellEditor tableEditor;

    public LogicalPropertyEditor(Object value, boolean threeState) {
        setHorizontalAlignment(JCheckBox.CENTER);

        setOpaque(true);
        setBackground(Color.white);

        this.threeState = threeState;
        // set new value because we'll finish editing immediately
        nextValue = getNextValue(value, threeState);
        //keep old value in editor, we don't want flashing - new value will be shown in renderer
        model.setSelected(value != null && (boolean) value);
        model.setEnabled(!threeState || value != null);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tableEditor.stopCellEditingLater();
            }
        });
    }

    private Boolean getNextValue(Object value, boolean threeState) {
        if (threeState) {
            if (value == null) return true;
            if ((boolean) value) return false;
            return null;
        } else {
            return value == null || !(boolean) value;
        }
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
        return threeState ? nextValue : (nextValue != null && nextValue ? true : null);
    }

    @Override
    public boolean stopCellEditing(){
        return true;
    }

    @Override
    public void cancelCellEditing() { }
}
