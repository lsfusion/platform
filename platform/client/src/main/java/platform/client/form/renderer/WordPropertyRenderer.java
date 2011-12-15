package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class WordPropertyRenderer extends FilePropertyRenderer
        implements PropertyRendererComponent {

    public WordPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("doc"));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void rateSelected() {
        super.paintSelected();
    }
}
