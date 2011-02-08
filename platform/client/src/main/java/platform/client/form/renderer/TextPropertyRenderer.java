package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.Format;


public class TextPropertyRenderer extends JTextArea implements PropertyRendererComponent {

    Format format;
    Color background;

    public TextPropertyRenderer(Format iformat, ComponentDesign design) {
        super();

        format = iformat;
        setBorder(new EmptyBorder(1, 3, 2, 2));
        setOpaque(true);
        setLineWrap(true);
        setFont(new Font("Tahoma", Font.PLAIN, 12));

        if (design != null)
            design.designCell(this);
        setEditable(false);
        background = getBackground();
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128, 128, 255));
            else
                setBackground(new Color(192, 192, 255));

        } else
            setBackground(background);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(value.toString());
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }
}
