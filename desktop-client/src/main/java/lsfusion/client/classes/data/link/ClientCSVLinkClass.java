package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.CSVLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.classes.DataType;

public class ClientCSVLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientCSVLinkClass instance = new ClientCSVLinkClass(false);

    public ClientCSVLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new CSVLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.CSVLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.csv.link");
    }
}
