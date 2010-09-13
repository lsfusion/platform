package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DocumentPropertyEditor;
import platform.client.form.renderer.WordPropertyRenderer;
import platform.interop.ComponentDesign;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientWordClass extends ClientFileClass {

    public ClientWordClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new WordPropertyRenderer(format, design);
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
}