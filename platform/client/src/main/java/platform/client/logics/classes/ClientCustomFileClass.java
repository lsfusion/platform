package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.CustomFileEditor;
import platform.client.form.editor.DocumentPropertyEditor;
import platform.client.form.renderer.CustomFileRenderer;
import platform.client.form.renderer.WordPropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;

public class ClientCustomFileClass extends ClientFileClass {

    public final static ClientCustomFileClass instance = new ClientCustomFileClass();

    private final String sID = "CustomFileClass";

    @Override
    public String getSID() {
        return sID;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new CustomFileRenderer(format, design);
    }

    public byte getTypeId() {
        return Data.CUSTOMFILECLASS;
    }

    @Override
    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new CustomFileEditor(value);
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
        return "Произвольный файл";
    }
}