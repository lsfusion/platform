package lsfusion.client.form.renderer.link;

import lsfusion.base.BaseUtils;
import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class DynamicFormatLinkRenderer extends LinkPropertyRenderer {

    public DynamicFormatLinkRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        super.setValue(value, isSelected, hasFocus);
        
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon(BaseUtils.getFileExtension((String) value)));
        }
    }
}