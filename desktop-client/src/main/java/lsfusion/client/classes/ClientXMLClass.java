package lsfusion.client.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.FilePropertyEditor;
import lsfusion.client.form.property.classes.renderer.XMLPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientXMLClass extends ClientStaticFormatFileClass {

    public final static ClientXMLClass instance = new ClientXMLClass(false, false);

    public ClientXMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"xml"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new XMLPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "XML";
    }

    public byte getTypeId() {
        return DataType.XML;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.xml"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.xml.file");
    }
}
