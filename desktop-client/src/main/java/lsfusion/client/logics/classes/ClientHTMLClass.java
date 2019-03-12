package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.FilePropertyEditor;
import lsfusion.client.form.renderer.HTMLPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientHTMLClass extends ClientStaticFormatFileClass {

    public final static ClientHTMLClass instance = new ClientHTMLClass(false, false);

    public ClientHTMLClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"html"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new HTMLPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "HTML";
    }

    public byte getTypeId() {
        return DataType.HTML;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.html"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.html.file");
    }
}
