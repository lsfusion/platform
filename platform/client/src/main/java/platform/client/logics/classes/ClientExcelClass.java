package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DocumentPropertyEditor;
import platform.client.form.renderer.ExcelPropertyRenderer;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;

public class ClientExcelClass extends ClientFileClass {

    public final static ClientExcelClass instance = new ClientExcelClass();

    private final String sID = "ExcelClass";

    @Override
    public String getSID() {
        return sID;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new ExcelPropertyRenderer(format, design);
    }

    public byte getTypeId() {
        return Data.EXCEL;
    }

    @Override
    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new DocumentPropertyEditor(value, ClientResourceBundle.getString("logics.classes.excel.documents"), "xls", "xlsx");
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
        return ClientResourceBundle.getString("logics.classes.excel.file");
    }
}