package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.FilePropertyEditor;
import lsfusion.client.form.property.classes.renderer.DynamicFormatFileRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientDynamicFormatFileClass extends ClientFileClass {

    public final static ClientDynamicFormatFileClass instance = new ClientDynamicFormatFileClass(false, false);

    public ClientDynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DynamicFormatFileRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "File";
    }

    public byte getTypeId() {
        return DataType.DYNAMICFORMATFILE;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.custom.file");
    }
}