package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.ExcelPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientExcelClass extends ClientStaticFormatFileClass {

    public final static ClientExcelClass instance = new ClientExcelClass(false, false);

    public ClientExcelClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"xls", "xlsx"};
    }

    @Override
    public String getDescription() {
        return ClientResourceBundle.getString("logics.classes.excel.documents");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ExcelPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return "Excel";
    }

    public byte getTypeId() {
        return DataType.EXCEL;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.excel.file");
    }
}