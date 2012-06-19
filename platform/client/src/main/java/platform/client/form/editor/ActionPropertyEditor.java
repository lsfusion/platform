package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.cell.PropertyTableCellEditor;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

public class ActionPropertyEditor implements PropertyEditorComponent {
    private ActionPropertyRenderer editorComponent;
    private PropertyTableCellEditor tableEditor;

    public ActionPropertyEditor(ClientPropertyDraw property) {
        //рисуем эдитор так же, как рендерер
        editorComponent = new ActionPropertyRenderer(property);
        editorComponent.setValue(true, true, true);

        editorComponent.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tableEditor.stopCellEditing();
            }
        });
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return editorComponent;
    }

    public Object getCellEditorValue() {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }
}
