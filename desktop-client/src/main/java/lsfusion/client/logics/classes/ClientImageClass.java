package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.FilePropertyEditor;
import lsfusion.client.form.renderer.ImagePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.awt.*;

public class ClientImageClass extends ClientStaticFormatFileClass {

    public final static ClientImageClass instance = new ClientImageClass(false, false);

    public ClientImageClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
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