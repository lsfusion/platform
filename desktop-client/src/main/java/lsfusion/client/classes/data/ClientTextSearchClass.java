package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.ActionPropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.StringPropertyRenderer;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;

import java.awt.*;
import java.text.ParseException;
import java.util.EventObject;

// a full-text search class, TSVECTOR or TSQUERY: its value is shown as text and is not edited
public abstract class ClientTextSearchClass extends ClientDataClass implements ClientTypeClass {

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("Full-text search classes don't support conversion from string", 0);
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return obj.toString();
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new PropertyEditor() {
            private ActionPropertyRenderer editorComponent;
            @Override
            public void setTableEditor(PropertyTableCellEditor tableEditor) { editorComponent = new ActionPropertyRenderer(property); }
            @Override
            public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) { return editorComponent.getComponent(); }
            @Override
            public Object getCellEditorValue() { return null; }
            @Override
            public boolean stopCellEditing() { return true; }
            @Override
            public void cancelCellEditing() {}
        };
    }

    @Override
    protected int getDefaultCharWidth() {
        return 15;
    }
}
