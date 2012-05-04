package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.logics.ClientPropertyDraw;

public class PDFPropertyRenderer extends FilePropertyRenderer {

    public PDFPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("pdf"));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
