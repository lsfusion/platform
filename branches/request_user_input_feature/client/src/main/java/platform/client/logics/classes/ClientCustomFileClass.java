package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.CustomFileRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;

public class ClientCustomFileClass extends ClientFileClass {

    public final static ClientCustomFileClass instance = new ClientCustomFileClass();

    private final String sID = "CustomFileClass";

    @Override
    public String getSID() {
        return sID;
    }

    public PropertyRendererComponent getRendererComponent(String caption, ClientPropertyDraw property) {
        return new CustomFileRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "File";
    }

    public byte getTypeId() {
        return Data.CUSTOMFILECLASS;
    }

    @Override
    public PropertyEditorComponent getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(false);
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