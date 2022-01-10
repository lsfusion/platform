package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.JSONPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientJSONFileClass extends ClientStaticFormatFileClass {

    public final static ClientJSONFileClass instance = new ClientJSONFileClass(false, false);

    public ClientJSONFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"json"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.json");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new JSONPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "JSON";
    }

    public byte getTypeId() {
        return DataType.JSONFILE;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.file");
    }
}
