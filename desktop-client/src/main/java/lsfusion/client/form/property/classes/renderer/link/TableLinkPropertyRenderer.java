package lsfusion.client.form.property.classes.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class TableLinkPropertyRenderer extends LinkPropertyRenderer {

    public TableLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("table"));
        }
        super.setValue(value);
    }
}