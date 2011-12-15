package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.ImagePropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.text.Format;

public class ClientImageClass extends ClientFileClass {

    public final static ClientImageClass instance = new ClientImageClass();

    private final String sID = "ImageClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.IMAGE;
    }

    public PropertyRendererComponent getRendererComponent(String caption, ClientPropertyDraw property) {
        return new ImagePropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "Image";
    }

    @Override
    public PropertyEditorComponent getComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(ClientResourceBundle.getString("logics.classes.image"), "jpg", "jpeg", "bmp", "png");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.image.file");
    }
}