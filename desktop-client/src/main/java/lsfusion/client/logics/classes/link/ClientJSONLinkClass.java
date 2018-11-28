package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.renderer.link.JSONLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

public class ClientJSONLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientJSONLinkClass instance = new ClientJSONLinkClass(false);

    public ClientJSONLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new JSONLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return Data.JSONLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.json.link");
    }
}
