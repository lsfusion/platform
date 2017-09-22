package lsfusion.client.form.renderer.link;

import lsfusion.base.BaseUtils;
import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class DynamicFormatLinkRenderer extends LinkPropertyRenderer {

    public DynamicFormatLinkRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon(BaseUtils.getFileExtension((String) value)));
        }
        super.setValue(value);
    }
}