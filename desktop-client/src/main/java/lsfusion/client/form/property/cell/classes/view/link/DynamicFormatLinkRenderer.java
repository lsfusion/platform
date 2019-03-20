package lsfusion.client.form.property.cell.classes.view.link;

import lsfusion.base.BaseUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

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