package lsfusion.client.form.property.classes.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class CSVLinkPropertyRenderer extends LinkPropertyRenderer {

    public CSVLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("csv"));
        }
        super.setValue(value);
    }
}