package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.text.Format;

public class PDFPropertyRenderer extends FilePropertyRenderer
    implements PropertyRendererComponent {

    private ImageIcon pdfIcon;

    public PDFPropertyRenderer(Format format, ComponentDesign design) {
        super(format, design);
        pdfIcon = new ImageIcon(PDFPropertyRenderer.class.getResource("/platform/images/pdf.jpeg"));
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setIcon(pdfIcon);
        } else {
            setIcon(null);
        }
        setSelected(isSelected, hasFocus);
    }
}
