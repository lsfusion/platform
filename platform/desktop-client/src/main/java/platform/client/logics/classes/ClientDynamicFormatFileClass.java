package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.DynamicFormatFileRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientDynamicFormatFileClass extends ClientFileClass {

    public final static ClientDynamicFormatFileClass instance = new ClientDynamicFormatFileClass();

    public ClientDynamicFormatFileClass() {
    }

    public ClientDynamicFormatFileClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String[] getExtensions() {
        return null;
    }

    public String getFileSID() {
        return "CustomFileClass";
    }

    public PropertyRendererComponent getRendererComponent(ClientPropertyDraw property) {
        return new DynamicFormatFileRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "File";
    }

    public byte getTypeId() {
        return Data.DYNAMICFORMATFILE;
    }

    @Override
    public PropertyEditorComponent getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, storeName);
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
        return ClientResourceBundle.getString("logics.classes.custom.file");
    }
}