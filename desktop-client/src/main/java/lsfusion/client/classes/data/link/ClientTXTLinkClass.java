package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.LinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientTXTLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientTXTLinkClass instance = new ClientTXTLinkClass(false);

    public ClientTXTLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new LinkPropertyRenderer(property, "txt");
    }

    public byte getTypeId() {
        return DataType.CSVLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.txt.link");
    }
}