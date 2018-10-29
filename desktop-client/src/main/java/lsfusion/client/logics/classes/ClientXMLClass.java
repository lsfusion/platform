package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.FilePropertyEditor;
import lsfusion.client.form.renderer.XMLPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

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
        return Data.XML;
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
