package lsfusion.client.form.renderer;

import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.Format;

public abstract class LabelPropertyRenderer extends JLabel implements PropertyRenderer {
    protected Format format;

    private Color defaultBackground = Color.WHITE;
    protected ClientPropertyDraw property;

    protected LabelPropertyRenderer(ClientPropertyDraw property) {
        super();
        this.property = property;
        if (property != null) {
            format = property.getFormat();
            setOpaque(true);
            property.design.designCell(this);
            defaultBackground = getBackground();
        }
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        drawBackground(isSelected, hasFocus);
        drawBorder(isSelected, hasFocus);
    }

    protected void drawBorder(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBorder(BorderFactory.createCompoundBorder(property.colorPreferences.getFocusedCellBorder(), BorderFactory.createEmptyBorder(0, 1, 0, 1)));
            } else {
                setBorder(new EmptyBorder(2, 2, 2, 2));
                setBorder(BorderFactory.createCompoundBorder(property.colorPreferences.getSelectedRowBorder(), BorderFactory.createEmptyBorder(1, 2, 1, 2)));
            }
        } else {
            setBorder(new EmptyBorder(1, 2, 1, 2));
        }
    }

    protected void drawBackground(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBackground(property.colorPreferences.getFocusedCellBackground());
            } else {
                setBackground(property.colorPreferences.getSelectedRowBackground());
            }
        } else {
            setBackground(defaultBackground);
        }
    }

    public void paintAsSelected() {
        setBackground(property.colorPreferences.getSelectedCellBackground());
    }
}
