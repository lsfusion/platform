package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class JSONPropertyRenderer extends FilePropertyRenderer {

    public JSONPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("json"));
        }
    }
}
