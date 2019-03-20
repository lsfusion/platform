package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.TableLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientTableLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientTableLinkClass instance = new ClientTableLinkClass(false);

    public ClientTableLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TableLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.TABLELINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.table.link");
    }
}