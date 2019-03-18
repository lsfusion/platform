package lsfusion.client.form.property.classes.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class XMLLinkPropertyRenderer extends LinkPropertyRenderer {

    public XMLLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("xml"));
        }
        super.setValue(value);
    }
}