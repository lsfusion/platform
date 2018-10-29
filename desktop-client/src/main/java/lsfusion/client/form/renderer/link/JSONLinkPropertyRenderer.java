package lsfusion.client.form.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class JSONLinkPropertyRenderer extends LinkPropertyRenderer {

    public JSONLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("json"));
        }
        super.setValue(value);
    }
}