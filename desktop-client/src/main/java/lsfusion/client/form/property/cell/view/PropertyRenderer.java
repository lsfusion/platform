package lsfusion.client.form.property.cell.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public abstract class PropertyRenderer {
    public static final String EMPTY_STRING = ClientResourceBundle.getString("form.renderer.not.defined");
    public static final String REQUIRED_STRING = ClientResourceBundle.getString("form.renderer.required");

    public static final Color REQUIRED_FOREGROUND = new Color(188, 63, 60);

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

        drawForeground(isInFocusedRow, hasFocus, conditionalForeground);

        drawBorder(isInFocusedRow, hasFocus);
    }
    
    protected boolean showRequiredString() {
        return false;
    }
    
    protected boolean showNotDefinedString() {
        return false;
    }
    
    protected Color getDefaultBackground() {
        return SwingDefaults.getTableCellBackground(); 
    }
    
    protected Border getDefaultBorder() {
        return SwingDefaults.getTableCellBorder();
    }

    protected void drawBackground(boolean isInFocusedRow, boolean hasFocus, Color conditionalBackground) {
        if (hasFocus) {
            getComponent().setBackground(SwingDefaults.getFocusedTableCellBackground());
        } else if (isInFocusedRow) {
            getComponent().setBackground(SwingDefaults.getFocusedTableRowBackground());
        } else if (conditionalBackground != null) {
            getComponent().setBackground(conditionalBackground);
        } else {
            getComponent().setBackground(getDefaultBackground());
        }
    }
    
    protected void drawForeground(boolean isInFocusedRow, boolean hasFocus, Color conditionalForeground) {
        if (value == null) {
            if (property != null && property.isEditableNotNull()) {
                if (showRequiredString()) {
                    getComponent().setForeground(REQUIRED_FOREGROUND);
                }
            } else if (showNotDefinedString()) {
                getComponent().setForeground(SwingDefaults.getNotDefinedForeground());
            }
        } else {
            if (hasFocus || isInFocusedRow) {
                getComponent().setForeground(SwingDefaults.getFocusedTableRowForeground());
            } else if (conditionalForeground != null) {
                getComponent().setForeground(conditionalForeground);
            } else {
                getComponent().setForeground(SwingDefaults.getTableCellForeground());
            }
        }
    }
    
    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus) {
        if (hasFocus) {
            getComponent().setBorder(SwingDefaults.getFocusedTableCellBorder());
        } else {
            getComponent().setBorder(getDefaultBorder());
        }
    }

    protected void paintAsSelected() {
        getComponent().setBackground(SwingDefaults.getTableSelectionBackground());
    }
}


