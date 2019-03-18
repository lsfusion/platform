package lsfusion.client.form.property.classes.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class PDFPropertyRenderer extends FilePropertyRenderer {

    public PDFPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (value != null) {
            getComponent().setIcon(SwingUtils.getSystemIcon("pdf"));
        }
    }
}
