package lsfusion.client.form.renderer.link;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class ExcelLinkPropertyRenderer extends LinkPropertyRenderer {

    public ExcelLinkPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        super.setValue(value, isSelected, hasFocus);
        
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("xls"));
        }
    }
}