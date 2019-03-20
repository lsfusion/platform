package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class WordLinkPropertyRenderer extends LinkPropertyRenderer {
    public WordLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("doc"));
        }
        super.setValue(value);
    }
}