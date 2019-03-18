package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.FilePropertyEditor;
import lsfusion.client.form.property.classes.renderer.PDFPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientPDFClass extends ClientStaticFormatFileClass {

    public final static ClientPDFClass instance = new ClientPDFClass(false, false);

    public ClientPDFClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"pdf"};
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new PDFPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "PDF";
    }

    public byte getTypeId() {
        return DataType.PDF;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.pdf"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.pdf.file");
    }
}
