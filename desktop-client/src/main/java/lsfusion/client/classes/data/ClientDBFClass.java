package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.DBFPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientDBFClass extends ClientStaticFormatFileClass {

    public final static ClientDBFClass instance = new ClientDBFClass(false, false);

    public ClientDBFClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"dbf"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DBFPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "DBF";
    }

    public byte getTypeId() {
        return DataType.DBF;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.dbf"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.dbf.file");
    }
}
