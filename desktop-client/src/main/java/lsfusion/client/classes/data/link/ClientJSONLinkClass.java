package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.link.LinkPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.classes.DataType;

public class ClientJSONLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientJSONLinkClass instance = new ClientJSONLinkClass(false);

    public ClientJSONLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new LinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.JSONLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.link");
    }
}
