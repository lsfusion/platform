package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class CSVPropertyRenderer extends FilePropertyRenderer {

    public CSVPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("csv"));
        }
    }
}
