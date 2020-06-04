package lsfusion.gwt.client.view;

import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class StyleDefaults {
    public static final int VALUE_HEIGHT = 20;
    public static final String VALUE_HEIGHT_STRING = VALUE_HEIGHT + "px";

    public static final int COMPONENT_HEIGHT = VALUE_HEIGHT + 2; // 2 for borders
    public static final String COMPONENT_HEIGHT_STRING = COMPONENT_HEIGHT + "px";

    public static final int CELL_VERTICAL_PADDING = 2; // suppose buttons have the same padding. to have equal height
    public static final int CELL_HORIZONTAL_PADDING = 3;
    public static final int BUTTON_HORIZONTAL_PADDING = 14;

    public static final int DEFAULT_FONT_PT_SIZE = 9;
    
    public static String selectedRowBackgroundColor;
    public static String focusedCellBackgroundColor;
    public static String focusedCellBorderColor;
    
    public static void reset() {
        selectedRowBackgroundColor = null;
        focusedCellBackgroundColor = null;
        focusedCellBorderColor = null;
    }

    private static String getSelectedColor(boolean canBeMixed) {
        if(canBeMixed) {
            // should be the same as '--selection-color' in <theme>.css.
            // can't use 'var(--selection-color)' because this color may be mixed with background color (converted to int)
            return colorTheme.isLight() ? "#D3E5E8" : "#2C4751";
        } else
            return "var(--selection-color)";
    }

    public static String getSelectedRowBackgroundColor(boolean canBeMixed) {
        if (selectedRowBackgroundColor == null) {
            ColorDTO preferredColor = MainFrame.colorPreferences.getSelectedRowBackground();
            if (preferredColor != null) {
                selectedRowBackgroundColor = getDisplayColor(preferredColor.toString());
            } else {
                // should be the same as '--selection-color' in <theme>.css. 
                // can't use 'var(--selection-color)' because this color may be mixed with background color (converted to int)
                selectedRowBackgroundColor = getSelectedColor(canBeMixed);
            }
        }
        return selectedRowBackgroundColor;
    }

    public static String getFocusedCellBackgroundColor(boolean canBeMixed) {
        if (focusedCellBackgroundColor == null) {
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBackground();
            if (preferredColor != null) {
                focusedCellBackgroundColor = getDisplayColor(preferredColor.toString());
            } else {
                focusedCellBackgroundColor = getSelectedColor(canBeMixed);
            }
        }
        return focusedCellBackgroundColor;
    }
    
    public static String getFocusedCellBorderColor() {
        if (focusedCellBorderColor == null) {
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBorderColor();
            if (preferredColor != null) {
                focusedCellBorderColor = getDisplayColor(preferredColor.toString());
            } else {
                focusedCellBorderColor = "var(--focus-color)";
            }
        }
        return focusedCellBorderColor;
    }

    
    // the following are copy-pasted colors from <color_theme>.css. need to be updated synchronously.    
    public static String getDefaultComponentBackground() {
        return "#FFFFFF";
    }

    public static String getComponentBackground(GColorTheme theme) {
        switch (theme) {
            case DARK:
                return "#45494A";
            default:
                return getDefaultComponentBackground();
        }
    }

    public static String getTextColor(GColorTheme theme) {
        switch (theme) {
            case DARK:
                return "#bbbbbb";
            default:
                return "#000000";
        }
    }
}
