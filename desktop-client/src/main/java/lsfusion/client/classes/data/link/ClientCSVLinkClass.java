package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.LinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientCSVLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientCSVLinkClass instance = new ClientCSVLinkClass(false);

    public ClientCSVLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new LinkPropertyRenderer(property, "csv");
    }

    public byte getTypeId() {
        return DataType.CSVLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.csv.link");
    }
}
