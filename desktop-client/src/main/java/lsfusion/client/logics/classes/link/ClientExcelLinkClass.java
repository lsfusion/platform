package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.ExcelLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

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