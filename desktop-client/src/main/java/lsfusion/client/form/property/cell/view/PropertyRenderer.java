package lsfusion.client.form.property.cell.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static javax.swing.BorderFactory.createEmptyBorder;
import static lsfusion.client.form.controller.ClientFormController.colorPreferences;

public abstract class PropertyRenderer {
    public static final String EMPTY_STRING = ClientResourceBundle.getString("form.renderer.not.defined");
    public static final String REQUIRED_STRING = ClientResourceBundle.getString("form.renderer.required");

    public static final Color NORMAL_FOREGROUND = UIManager.getColor("TextField.foreground");
    public static final Color INACTIVE_FOREGROUND = UIManager.getColor("TextField.inactiveForeground");
    public static final Color REQUIRED_FOREGROUND = new Color(136, 9, 0);

//    protected Color defaultBackground;

    protected ClientPropertyDraw property;
    protected Object value;

    public PropertyRenderer(ClientPropertyDraw property) {
        this.property = property;
        initDesign();
    }
    public abstract JComponent getComponent();

    protected void initDesign() {
        if (property != null) {
            property.design.designCell(getComponent());
        }
//        defaultBackground = null;
//        defaultBackground = getComponent().getBackground();
    }

    protected void setValue(Object value) {
        this.value = value;
    }

    public void updateRenderer(Object value, boolean isInFocusedRow, boolean hasFocus) {
        updateRenderer(value, isInFocusedRow, hasFocus, false, null, null);
    }

    public void updateRenderer(Object value, boolean isInFocusedRow, boolean hasFocus, boolean isSelected, Color conditionalBackground, Color conditionalForeground) {
        setValue(value);

        if (isSelected && !hasFocus) {
            paintAsSelected();
        } else {
            drawBackground(isInFocusedRow, hasFocus, conditionalBackground);
        }

        drawForeground(conditionalForeground);

        drawBorder(isInFocusedRow, hasFocus);
    }
    
//    protected Color getDefaultBackground() {
//        return defaultBackground;
//    }

    protected void drawBackground(boolean isInFocusedRow, boolean hasFocus, Color conditionalBackground) {
        Color baseColor = conditionalBackground != null ? conditionalBackground : null;
//        Color baseColor = conditionalBackground != null ? conditionalBackground : getComponent().getParent().getBackground();
//        Color baseColor = conditionalBackground != null ? conditionalBackground : getDefaultBackground();
        if (hasFocus) {
            getComponent().setBackground(baseColor != null ? new Color(baseColor.getRGB() & colorPreferences.getFocusedCellBackground().getRGB()) : colorPreferences.getFocusedCellBackground());
        } else if (isInFocusedRow) {
            getComponent().setBackground(baseColor != null ? new Color(baseColor.getRGB() & colorPreferences.getSelectedRowBackground().getRGB()) : colorPreferences.getSelectedRowBackground());
        } else {
            getComponent().setBackground(baseColor);
        }
    }
    
    protected void drawForeground(Color conditionalForeground) {
        if (conditionalForeground != null) {
            getComponent().setForeground(conditionalForeground);
        }    
    }
    
    protected Border getDefaultBorder() {
        return createEmptyBorder();    
    }

    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus) {
        if (hasFocus) {
            getComponent().setBorder(colorPreferences.getFocusedCellBorder());
        } else if (isInFocusedRow) {
            getComponent().setBorder(colorPreferences.getSelectedRowBorder());
        } else {
            getComponent().setBorder(getDefaultBorder());
        }
    }

    protected void paintAsSelected() {
        getComponent().setBackground(colorPreferences.getSelectedCellBackground());
    }
}


