package lsfusion.gwt.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import static lsfusion.gwt.client.base.view.ColorUtils.*;
import static lsfusion.gwt.client.view.MainFrame.colorTheme;

public class StyleDefaults {
    public static int maxMobileWidthHeight = 600;

    public static final int VALUE_HEIGHT = 20;
    public static final String VALUE_HEIGHT_STRING = VALUE_HEIGHT + "px";

    public static final int COMPONENT_HEIGHT = VALUE_HEIGHT + 2; // 2 for borders
    public static final String COMPONENT_HEIGHT_STRING = COMPONENT_HEIGHT + "px";

    public static final int CELL_HORIZONTAL_PADDING = 3;
    public static final int CELL_VERTICAL_PADDING = 2;
    public static final int BUTTON_HORIZONTAL_PADDING = 14;

    private static String selectedRowBackgroundColor;
    private static String focusedCellBackgroundColor;
    private static String focusedCellBorderColor;
    private static String tableGridColor;

    private static int[] componentBackgroundRGB;
    
    private static int[] pivotGroupLevelDarkenStepRGB;

    public static void init() {
        setCustomProperties(RootPanel.get().getElement(), getTableGridColor(), getFocusedCellBorderColor(), getSelectedRowBackgroundColor(), getFocusedCellBackgroundColor());
    }

    private static native void setCustomProperties(Element root, String tableGridColor, String focusedCellBorderColor, String selectedRowBackgroundColor, String focusedCellBackgroundColor) /*-{
        if(tableGridColor != null) {
            root.style.setProperty("--grid-separator-border-color", tableGridColor);
        }
        root.style.setProperty("--focused-cell-border-color", focusedCellBorderColor);
        root.style.setProperty("--selected-row-background-color", selectedRowBackgroundColor)
        root.style.setProperty("--focused-cell-background-color", focusedCellBackgroundColor)
    }-*/;

    public static void reset() {
        selectedRowBackgroundColor = null;
        focusedCellBackgroundColor = null;
        focusedCellBorderColor = null;
        tableGridColor = null;
        componentBackgroundRGB = null;
        pivotGroupLevelDarkenStepRGB = null;

        init();
    }

    public static String getSelectedRowBackgroundColor() {
        if (selectedRowBackgroundColor == null && MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getSelectedRowBackground();
            selectedRowBackgroundColor = preferredColor != null ? getThemedColor(preferredColor.toString()) : null;
        }
        return selectedRowBackgroundColor;
    }

    public static String getFocusedCellBackgroundColor() {
        if (focusedCellBackgroundColor == null && MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBackground();
            focusedCellBackgroundColor = preferredColor != null ? getThemedColor(preferredColor.toString()) : null;
        }
        return focusedCellBackgroundColor;
    }
    
    public static String getFocusedCellBorderColor() {
        if (focusedCellBorderColor == null && MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBorderColor();
            focusedCellBorderColor = preferredColor != null ? getThemedColor(preferredColor.toString()) : null;
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
