package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class ExcelPropertyRenderer extends FilePropertyRenderer
        implements PropertyRendererComponent {

    private ImageIcon excelIcon;

    public ExcelPropertyRenderer(Format format, ComponentDesign design) {
        super(format, design);
        excelIcon = new ImageIcon(WordPropertyRenderer.class.getResource("/platform/images/excel.jpeg"));
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(excelIcon);
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
