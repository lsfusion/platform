package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.ExcelLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.classes.DataType;

public class ClientExcelLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientExcelLinkClass instance = new ClientExcelLinkClass(false);

    public ClientExcelLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ExcelLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.EXCELLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.excel.link");
    }
}