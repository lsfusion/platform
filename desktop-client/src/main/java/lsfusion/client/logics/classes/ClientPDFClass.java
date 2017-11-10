package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.FilePropertyEditor;
import lsfusion.client.form.renderer.PDFPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.awt.*;

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
        return Data.PDF;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.pdf"), getExtensions());
    }

    @Override
    public int getPreferredHeight(FontMetrics font) {
        return 18;
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics font) {
        return 18;
    }

    @Override
    public int getMinimumWidth(int minCharWidth, FontMetrics font) {
        return 15;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.pdf.file");
    }
}
