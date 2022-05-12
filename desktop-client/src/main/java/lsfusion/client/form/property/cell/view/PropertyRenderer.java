package lsfusion.client.form.property.cell.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static lsfusion.client.base.view.ClientColorUtils.getThemedColor;
import static lsfusion.client.base.view.SwingDefaults.getFocusedTableCellBackground;
import static lsfusion.client.base.view.SwingDefaults.getFocusedTableRowBackground;

public abstract class PropertyRenderer {
    public static final String EMPTY_STRING = ClientResourceBundle.getString("form.renderer.empty");
    public static final String NOT_DEFINED_STRING = ClientResourceBundle.getString("form.renderer.not.defined");
    public static final String REQUIRED_STRING = ClientResourceBundle.getString("form.renderer.required");

    protected ClientPropertyDraw property;
    protected Object value;

    public PropertyRenderer(ClientPropertyDraw property) {
        this.property = property;
        if (property != null) {
            property.design.installFont(getComponent());
        }
    }
    public abstract JComponent getComponent();

    protected void setValue(Object value) {
        this.value = value;
    }

    public void updateRenderer(Object value, boolean isInFocusedRow, boolean hasFocus, boolean drawFocusBorder, boolean tableFocused) {
        updateRenderer(value, isInFocusedRow, hasFocus, drawFocusBorder, false, false, tableFocused, null, null, null);
    }

    public void updateRenderer(Object value,
                               boolean isInFocusedRow,
                               boolean hasFocus,
                               boolean drawFocusBorder,
                               boolean isSelected,
                               boolean hasSingleSelection,
                               boolean isTableFocused,
                               Color conditionalBackground,
                               Color conditionalForeground,
                               Image conditionalImage) {
        setValue(value);

        if (!isSelected || (hasSingleSelection && (hasFocus && isTableFocused || !isTableFocused && isInFocusedRow))) {
            drawBackground(isInFocusedRow, hasFocus, conditionalBackground);
        } else {
            paintAsSelected();
        }

        drawForeground(isInFocusedRow, hasFocus, conditionalForeground);

        drawImage(conditionalImage);

        drawBorder(isInFocusedRow, hasFocus, drawFocusBorder);
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
        // to achieve 'themed' color base color should be converted with getThemedColor() only once
        // mix and convert calls order in not important
        // notice: getFocusedTableCellBackground() and getFocusedTableRowBackground() return already themed color - no need of additional conversion
        
        Color logicsBackground = conditionalBackground;
        if (logicsBackground == null && property != null) {
            logicsBackground = property.design.background;
        }
        
        if (hasFocus) {
            // for now focus color is not mixed with base cell color - as it is done in panel
            getComponent().setBackground(logicsBackground != null ? getThemedColor(logicsBackground) : getFocusedTableCellBackground());
        } else if (isInFocusedRow) {
            final Color focusedRowBackground = getFocusedTableRowBackground();
            getComponent().setBackground(logicsBackground != null ? new Color(focusedRowBackground.getRGB() & logicsBackground.getRGB()) : focusedRowBackground);
        } else if (logicsBackground != null) {
            getComponent().setBackground(getThemedColor(logicsBackground));
        } else {
            getComponent().setBackground(getDefaultBackground());
        }
    }
    
    protected void drawForeground(boolean isInFocusedRow, boolean hasFocus, Color conditionalForeground) {
        if (value == null) {
            if (property != null && property.isEditableNotNull()) {
                if (showRequiredString()) {
                    getComponent().setForeground(SwingDefaults.getRequiredForeground());
                }
            } else if (showNotDefinedString()) {
                getComponent().setForeground(SwingDefaults.getNotDefinedForeground());
            }
        } else {
            if (conditionalForeground != null) {
                getComponent().setForeground(getThemedColor(conditionalForeground));
            } else if (property != null && property.design.foreground != null) {
                getComponent().setForeground(getThemedColor(property.design.foreground));
            } else {
                getComponent().setForeground(SwingDefaults.getTableCellForeground());
            }
        }
    }

    protected void drawImage(Image conditionalImage) {
        JComponent component = getComponent();
        if (component instanceof JButton && conditionalImage != null) {
            ((JButton) component).setIcon(new ImageIcon(conditionalImage));
        }
    }
    
    protected void drawBorder(boolean isInFocusedRow, boolean hasFocus, boolean drawFocusBorder) {
        if (drawFocusBorder && hasFocus) {
            getComponent().setBorder(SwingDefaults.getFocusedTableCellBorder());
        } else {
            getComponent().setBorder(getDefaultBorder());
        }
    }

    protected void paintAsSelected() {
        getComponent().setBackground(SwingDefaults.getTableSelectionBackground());
    }

    protected String getRequiredStringValue() {
        return MainController.showNotDefinedStrings ? REQUIRED_STRING : "";
    }

    public boolean isAutoDynamicHeight() {
        return true;
    }
}


