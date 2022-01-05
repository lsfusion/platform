package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;

public class ClientNamedFileClass extends ClientDynamicFormatFileClass {

    public final static ClientDynamicFormatFileClass instance = new ClientNamedFileClass(false, false);

    public ClientNamedFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String formatString(Object obj) {
        return "NamedFile";
    }

    public byte getTypeId() {
        return DataType.NAMEDFILE;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new FilePropertyEditor(multiple, storeName, true);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.named.file");
    }
}