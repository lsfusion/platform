package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.FilePropertyEditor;
import lsfusion.client.form.renderer.TablePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientTableClass extends ClientStaticFormatFileClass {

    public final static ClientTableClass instance = new ClientTableClass(false, false);

    public ClientTableClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"table"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TablePropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "table";
    }

    public byte getTypeId() {
        return DataType.TABLE;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.table"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.table.file");
    }
}
