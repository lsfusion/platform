package lsfusion.client.form.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class PDFLinkPropertyRenderer extends LinkPropertyRenderer {

    public PDFLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("pdf"));
        }
        super.setValue(value, isSelected, hasFocus);
    }
}