package lsfusion.client.form.editor;

import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.cell.PropertyTableCellEditor;

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

        //ставим новое значение, т.к. сразу закончим редактирование
        newValue = value == null;
        model.setSelected(newValue);

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tableEditor.stopCellEditing();
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
