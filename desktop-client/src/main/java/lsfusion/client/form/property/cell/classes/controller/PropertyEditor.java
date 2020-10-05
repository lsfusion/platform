package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import java.awt.*;
import java.util.EventObject;

public interface PropertyEditor {

    void setTableEditor(PropertyTableCellEditor tableEditor);

    Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent);

    Object getCellEditorValue();

    boolean stopCellEditing();
    void cancelCellEditing();
}


