package lsfusion.client.form.property.classes.editor;

import lsfusion.client.form.property.cell.PropertyTableCellEditor;

import java.awt.*;
import java.util.EventObject;

public interface PropertyEditor {

    void setTableEditor(PropertyTableCellEditor tableEditor);

    Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent);

    Object getCellEditorValue();

    boolean stopCellEditing();
}


