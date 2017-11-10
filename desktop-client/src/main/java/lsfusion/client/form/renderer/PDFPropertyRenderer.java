package lsfusion.client.form.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class PDFPropertyRenderer extends FilePropertyRenderer {

    public PDFPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        super.setValue(value, isSelected, hasFocus);
        
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("pdf"));
        }
    }
}
