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

public class ClientJSONClass extends ClientAJSONClass {

    public byte getTypeId() {
        return DataType.JSON;
    }

    public final static ClientJSONClass instance = new ClientJSONClass();

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json");
    }
}