package lsfusion.gwt.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import static lsfusion.gwt.client.base.view.ColorUtils.*;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class StyleDefaults {
    public static int maxMobileWidthHeight = 570;

    public static final int VALUE_HEIGHT = 20;
    public static final String VALUE_HEIGHT_STRING = VALUE_HEIGHT + "px";

    public static final int COMPONENT_HEIGHT = VALUE_HEIGHT + 2; // 2 for borders
    public static final String COMPONENT_HEIGHT_STRING = COMPONENT_HEIGHT + "px";

    public static final int TEXT_MULTILINE_PADDING = 2; // since there are a lot of lines and their rendering takes a lot of space give some extra padding

    public static final int CELL_HORIZONTAL_PADDING = 3;
    public static final int BUTTON_HORIZONTAL_PADDING = 14;
    
    public static final int DATA_PANEL_LABEL_MARGIN = 4;

    public static final int DEFAULT_FONT_PT_SIZE = 9;

    private static String selectedRowBackgroundColor;
    private static String selectedRowBackgroundColorMixed;
    private static String focusedCellBackgroundColor;
    private static String focusedCellBackgroundColorMixed;
    private static String focusedCellBorderColor;
    private static String tableGridColor;

    private static int[] componentBackgroundRGB;
    
    private static int[] pivotGroupLevelDarkenStepRGB;

    public static void init() {
        setCustomProperties(RootPanel.get().getElement(), getTableGridColor(), getFocusedCellBorderColor());
    }

    private static native void setCustomProperties(Element root, String tableGridColor, String focusedCellBorderColor) /*-{
        if(tableGridColor != null) {
            root.style.setProperty("--grid-separator-border-color", tableGridColor);
        }
        root.style.setProperty("--focused-cell-border-color", focusedCellBorderColor);
    }-*/;

    public static void reset() {
        selectedRowBackgroundColor = null;
        selectedRowBackgroundColorMixed = null;
        focusedCellBackgroundColor = null;
        focusedCellBackgroundColorMixed = null;
        focusedCellBorderColor = null;
        tableGridColor = null;
        componentBackgroundRGB = null;
        pivotGroupLevelDarkenStepRGB = null;

        init();
    }

    private static String getSelectedColor(boolean canBeMixed) {
        if(canBeMixed) {
            // should be the same as '--selection-color' in <theme>.css.
            // can't use 'var(--selection-color)' because this color may be mixed with background color (converted to int)
            return colorTheme.isLight() ? "#D3E5E8" : "#2C4751";
        } else
            return "var(--selection-color)";
    }

    public static String calculateSelectedRowBackgroundColor(boolean canBeMixed) {
        ColorDTO preferredColor = MainFrame.colorPreferences.getSelectedRowBackground();
        if (preferredColor != null)
            return getThemedColor(preferredColor.toString());

        // should be the same as '--selection-color' in <theme>.css.
        // can't use 'var(--selection-color)' because this color may be mixed with background color (converted to int)
        return getSelectedColor(canBeMixed);
    }

    public static String calculateFocusedCellBackgroundColor(boolean canBeMixed) {
        ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBackground();
        if (preferredColor != null)
            return getThemedColor(preferredColor.toString());

        return getSelectedColor(canBeMixed);
    }

    public static String getSelectedRowBackgroundColor(boolean canBeMixed) {
        if(canBeMixed) {
            if (selectedRowBackgroundColorMixed == null)
                selectedRowBackgroundColorMixed = calculateSelectedRowBackgroundColor(true);
            return selectedRowBackgroundColorMixed;
        }

        if (selectedRowBackgroundColor == null)
            selectedRowBackgroundColor = calculateSelectedRowBackgroundColor(false);
        return selectedRowBackgroundColor;
    }

    public static String getFocusedCellBackgroundColor(boolean canBeMixed) {
        if(canBeMixed) {
            if (focusedCellBackgroundColorMixed == null)
                focusedCellBackgroundColorMixed = calculateFocusedCellBackgroundColor(true);
            return focusedCellBackgroundColorMixed;
        }

        if (focusedCellBackgroundColor == null)
            focusedCellBackgroundColor = calculateFocusedCellBackgroundColor(false);
        return focusedCellBackgroundColor;
    }

    public static String getFocusColor(boolean canBeMixed) {
        if (canBeMixed) {
            // should be the same as '--focus-color' in <theme>.css.
            // can't use 'var(--focus-color)' because this color may be mixed with other color (converted to int)
            return colorTheme.isLight() ? "#0489BA" : "#0790c3";
        } else
            return "var(--focus-color)";
    }
    
    public static String getFocusedCellBorderColor() {
        if (focusedCellBorderColor == null && MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBorderColor();
            if (preferredColor != null) {
                focusedCellBorderColor = getThemedColor(preferredColor.toString());
            } else {
                focusedCellBorderColor = getFocusColor(false);
            }
        }
        return focusedCellBorderColor;
    }
    
    public static String getTableGridColor() {
        if (tableGridColor == null && MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getTableGridColor();
            tableGridColor = preferredColor != null ? getThemedColor(preferredColor.toString()) : null;
        }
        return tableGridColor;
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

    public static String getGridSeparatorBorderColor(GColorTheme theme) {
        switch (theme) {
            case DARK:
                return "#5E6364";
            default:
                return "#E6E6E6";
        }
    }
}
