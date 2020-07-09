package lsfusion.gwt.client.view;

import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import static lsfusion.gwt.client.base.view.ColorUtils.*;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class StyleDefaults {
    public static int maxMobileWidth = 600;

    public static final int VALUE_HEIGHT = 20;
    public static final String VALUE_HEIGHT_STRING = VALUE_HEIGHT + "px";

    public static final int COMPONENT_HEIGHT = VALUE_HEIGHT + 2; // 2 for borders
    public static final String COMPONENT_HEIGHT_STRING = COMPONENT_HEIGHT + "px";

    public static final int TEXT_MULTILINE_PADDING = 2; // since there are a lot of lines and their rendering takes a lot of space give some extra padding

    public static final int CELL_HORIZONTAL_PADDING = 3;
    public static final int BUTTON_HORIZONTAL_PADDING = 14;

    public static final int DEFAULT_FONT_PT_SIZE = 9;
    
    public static String selectedRowBackgroundColor;
    public static String focusedCellBackgroundColor;
    public static String focusedCellBorderColor;
    
    public static int[] componentBackgroundRGB;
    
    public static int[] pivotGroupLevelDarkenStepRGB;
    
    public static void reset() {
        selectedRowBackgroundColor = null;
        focusedCellBackgroundColor = null;
        focusedCellBorderColor = null;
        componentBackgroundRGB = null;
        pivotGroupLevelDarkenStepRGB = null;
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
    
    public static int[] getComponentBackgroundRGB() {
        if (componentBackgroundRGB == null) {
            int cbRGB = toRGB(getComponentBackground(colorTheme));
            componentBackgroundRGB = new int[]{getRed(cbRGB), getGreen(cbRGB), getBlue(cbRGB)};
        }
        return componentBackgroundRGB;
    }
    
    public static int[] getPivotGroupLevelDarkenStepRGB() {
        if (pivotGroupLevelDarkenStepRGB == null) {
            int pbRGB = toRGB(getPanelBackground(colorTheme));
            int[] panelRGB = new int[]{getRed(pbRGB), getGreen(pbRGB), getBlue(pbRGB)};
            int[] componentRGB = getComponentBackgroundRGB();
            
            float darkenStep = 0.8f;

            pivotGroupLevelDarkenStepRGB = new int[]{
                    (int) ((panelRGB[0] - componentRGB[0]) * darkenStep),
                    (int) ((panelRGB[1] - componentRGB[1]) * darkenStep),
                    (int) ((panelRGB[2] - componentRGB[2]) * darkenStep)
            };
        }
        return pivotGroupLevelDarkenStepRGB;
    }

    
    // the following are copy-pasted colors from <color_theme>.css. need to be updated synchronously.
    // maybe getComputedStyle(document.documentElement).getPropertyValue() should be used instead where possible
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
    
    public static String getPanelBackground(GColorTheme theme) {
        switch (theme) {
            case DARK:
                return "#3C3F41";
            default:
                return "#F2F2F2";
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
