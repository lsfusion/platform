package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.DBFLinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientDBFLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientDBFLinkClass instance = new ClientDBFLinkClass(false);

    public ClientDBFLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DBFLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.DBFLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.dbf.link");
    }
}
