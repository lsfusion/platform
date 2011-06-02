package platform.client.form.renderer;

import platform.client.SwingUtils;
import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class ExcelPropertyRenderer extends FilePropertyRenderer
        implements PropertyRendererComponent {

    public ExcelPropertyRenderer(Format format, ComponentDesign design) {
        super(format, design);
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(SwingUtils.getSystemIcon("xls"));
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
