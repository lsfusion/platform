package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.controller.FilePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.PDFPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
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
