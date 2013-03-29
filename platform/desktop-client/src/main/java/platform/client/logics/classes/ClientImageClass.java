package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.ImagePropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientImageClass extends ClientStaticFormatFileClass {

    public final static ClientImageClass instance = new ClientImageClass();

    public ClientImageClass() {
    }

    public ClientImageClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"jpg", "jpeg", "bmp", "png"};
    }

    public String getFileSID() {
        return "ImageClass";
    }

    public byte getTypeId() {
        return Data.IMAGE;
    }

    @Override
    public int getMaximumHeight(FontMetrics fontMetrics) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics) {
        return Integer.MAX_VALUE;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ImagePropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "Image";
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.image"), getExtensions());
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.image.file");
    }
}