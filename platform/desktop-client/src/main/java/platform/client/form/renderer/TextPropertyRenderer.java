package platform.client.form.renderer;

import platform.client.form.PropertyRenderer;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.text.Format;


public class TextPropertyRenderer extends JTextArea implements PropertyRenderer {

    Format format;

    private Color defaultBackground = Color.WHITE;

    public TextPropertyRenderer(ClientPropertyDraw property) {
        super();

        format = property.getFormat();
        setOpaque(true);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFont(new Font("Tahoma", Font.PLAIN, 10));

        if (property.design != null) {
            property.design.designCell(this);
        }
        defaultBackground = getBackground();
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
            setBackground(defaultBackground);
        }
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null) {
            setText(value.toString());
            setForeground(UIManager.getColor("TextField.foreground"));
        }
        else {
            setText(EMPTY_STRING);
            setForeground(UIManager.getColor("TextField.inactiveForeground"));
        }
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void paintAsSelected() {
        setBackground(PropertyRenderer.SELECTED_CELL_BACKGROUND);
    }
}
