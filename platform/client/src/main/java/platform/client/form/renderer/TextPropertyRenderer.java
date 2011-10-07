package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;
import java.text.Format;


public class TextPropertyRenderer extends JTextArea implements PropertyRendererComponent {

    Format format;

    public TextPropertyRenderer(Format iformat, ComponentDesign design) {
        super();

        format = iformat;
        setOpaque(true);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(new Font("Tahoma", Font.PLAIN, 10));

        if (design != null)
            design.designCell(this);
        setEditable(false);
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBorder(BorderFactory.createCompoundBorder(FOCUSED_CELL_BORDER, BorderFactory.createEmptyBorder(1, 2, 0, 1)));
                setBackground(FOCUSED_CELL_BACKGROUND);
            }
            else {
                setBorder(BorderFactory.createCompoundBorder(SELECTED_ROW_BORDER, BorderFactory.createEmptyBorder(1, 3, 0, 2)));
                setBackground(SELECTED_ROW_BACKGROUND);
            }
        } else {
            setBorder(BorderFactory.createEmptyBorder(2, 3, 1, 2));
            setBackground(Color.WHITE);
        }
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

    @Override
    public void rateSelected() {
        setBackground(PropertyRendererComponent.SELECTED_CELL_BACKGROUND);
    }
}
