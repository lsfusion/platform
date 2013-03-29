package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.PDFPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientPDFClass extends ClientStaticFormatFileClass {

    public final static ClientPDFClass instance = new ClientPDFClass();

    public ClientPDFClass() {
    }

    public ClientPDFClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"pdf"};
    }

    public String getFileSID() {
        return "PDFClass";
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
