package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.Format;

class LabelPropertyRenderer extends JLabel { //DefaultTableCellRenderer {

    Format format;

    LabelPropertyRenderer(Format iformat, ComponentDesign design) {
        super();

        format = iformat;
        setOpaque(true);

        if (design != null)
            design.designCell(this);
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBorder(BorderFactory.createCompoundBorder(PropertyRendererComponent.FOCUSED_CELL_BORDER, BorderFactory.createEmptyBorder(0, 1, 0, 1)));
                setBackground(PropertyRendererComponent.FOCUSED_CELL_BACKGROUND);
            }
            else {
                setBorder(BorderFactory.createCompoundBorder(PropertyRendererComponent.SELECTED_CELL_BORDER, BorderFactory.createEmptyBorder(1, 2, 1, 2)));
                setBackground(PropertyRendererComponent.SELECTED_CELL_BACKGROUND);
            }
        } else {
            setBorder(new EmptyBorder(1, 2, 1, 2));
            setBackground(Color.WHITE);
        }
    }

}
