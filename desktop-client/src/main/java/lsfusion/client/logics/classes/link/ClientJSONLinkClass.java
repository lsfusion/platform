package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.JSONLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientJSONLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientJSONLinkClass instance = new ClientJSONLinkClass(false);

    public ClientJSONLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new JSONLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.JSONLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.link");
    }
}
