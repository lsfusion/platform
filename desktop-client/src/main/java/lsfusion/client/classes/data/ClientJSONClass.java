package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.JSONPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientJSONClass extends ClientStaticFormatFileClass {

    public final static ClientJSONClass instance = new ClientJSONClass(false, false);

    public ClientJSONClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"json"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new JSONPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "JSON";
    }

    public byte getTypeId() {
        return DataType.JSON;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.json"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.file");
    }
}
