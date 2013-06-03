package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.FilePropertyEditor;
import lsfusion.client.form.renderer.ExcelPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.awt.*;

public class ClientExcelClass extends ClientStaticFormatFileClass {

    public final static ClientExcelClass instance = new ClientExcelClass(false, false);

    public ClientExcelClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
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