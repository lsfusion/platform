package lsfusion.client.form.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class WordLinkPropertyRenderer extends LinkPropertyRenderer {
    public WordLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("doc"));
        }
        super.setValue(value, isSelected, hasFocus);
    }
}