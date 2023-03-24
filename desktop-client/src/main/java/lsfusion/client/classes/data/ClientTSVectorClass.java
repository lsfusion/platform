package lsfusion.client.classes.data;

import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.ActionPropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.StringPropertyRenderer;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

import java.awt.*;
import java.text.ParseException;
import java.util.EventObject;

public class ClientTSVectorClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientTSVectorClass instance = new ClientTSVectorClass();

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new StringPropertyRenderer(property);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        throw new ParseException("TSVector class doesn't support conversion from string", 0);
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return obj.toString();
    }

    @Override
    public byte getTypeId() {
        return DataType.TSVECTOR;
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
