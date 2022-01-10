package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TextPropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TextPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

import java.text.ParseException;

public class ClientJSONClass extends ClientDataClass implements ClientTypeClass {
    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TextPropertyRenderer(property, false);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return (String) obj;
    }

    public byte getTypeId() {
        return DataType.JSON;
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return  new TextPropertyEditor(value, property.design);
    }

    public final static ClientJSONClass instance = new ClientJSONClass();

    @Override
    protected int getDefaultCharWidth() {
        return 15;
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json");
    }
}