package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.ExcelPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientExcelClass extends ClientStaticFormatFileClass {

    public final static ClientExcelClass instance = new ClientExcelClass();

    public ClientExcelClass() {
    }

    public ClientExcelClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"xls", "xlsx"};
    }

    public String getFileSID() {
        return "ExcelClass";
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ExcelPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "Excel";
    }

    public byte getTypeId() {
        return Data.EXCEL;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw design) {
        return new FilePropertyEditor(multiple, storeName, ClientResourceBundle.getString("logics.classes.excel.documents"), getExtensions());
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