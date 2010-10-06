package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DocumentPropertyEditor;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.ExcelPropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientExcelClass extends ClientFileClass {

    public ClientExcelClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new ExcelPropertyRenderer(format, design);
    }

    @Override
    public byte getTypeId() {
        return Data.EXCEL;
    }

    @Override
    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new DocumentPropertyEditor(value, "Документы Excel", "xls", "xlsx");
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