package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.CSVLinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientTXTLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientTXTLinkClass instance = new ClientTXTLinkClass(false);

    public ClientTXTLinkClass(boolean multiple) {
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
        return ClientResourceBundle.getString("logics.classes.txt.link");
    }
}