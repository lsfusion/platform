package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DocumentPropertyEditor;
import platform.client.form.renderer.WordPropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;

public class ClientWordClass extends ClientFileClass {

    public final static ClientWordClass instance = new ClientWordClass();

    private final String sID = "WordClass";

    @Override
    public String getSID() {
        return sID;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new WordPropertyRenderer(format, design);
    }

    public byte getTypeId() {
        return Data.WORD;
    }

    @Override
    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new DocumentPropertyEditor(value, "Документы Word", "doc", "docx");
    }

    @Override
    public int getPreferredHeight(FontMetrics font) {
        return 18;
    }

    @Override
    public int getPreferredWidth(FontMetrics font) {
        return 18;
    }

    @Override
    public int getMinimumWidth(FontMetrics font) {
        return 15;
    }

    @Override
    public String toString() {
        return "Файл Ворд";
    }
}