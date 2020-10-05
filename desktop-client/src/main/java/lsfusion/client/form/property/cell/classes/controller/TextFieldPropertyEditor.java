package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.EventObject;

import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;

public abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditor {
    private static final String CANCEL_EDIT_ACTION = "reset-field-edit";

    protected PropertyTableCellEditor tableEditor;

    TextFieldPropertyEditor(ClientPropertyDraw property) {
        super();

        Insets insets = getTableCellMargins();
        setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1));
        setOpaque(true);

        if (property != null) {
            if (property.design != null)
                ClientColorUtils.designComponent(this, property.design);

            Integer valueAlignment = property.getSwingValueAlignment();
            if (valueAlignment != null) {
                setHorizontalAlignment(valueAlignment);
            }
        }

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableEditor.stopCellEditing();
            }
        });

        getActionMap().put(CANCEL_EDIT_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableEditor.cancelCellEditing();
            }
        });
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    public boolean stopCellEditing() {
        try {
            commitEdit();
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    public void cancelCellEditing() { }

    public Object getCellEditorValue() {
        return this.getValue();
    }

    @Override
    public String toString() {
        if (tableEditor != null) {
            return "TextFieldEditor[" + tableEditor.getTable().getName() + "]: " + super.toString();
        } else {
            return super.toString();
        }
    }

    @Override
    public void replaceSelection(String content){
        if(content.endsWith("\n"))
            content = content.substring(0, content.length()-1);
        try {
            super.replaceSelection(content);
        } catch (IllegalArgumentException e) { // strange java bug, so just suppress that exception
        }
    }
}
