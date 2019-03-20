package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

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