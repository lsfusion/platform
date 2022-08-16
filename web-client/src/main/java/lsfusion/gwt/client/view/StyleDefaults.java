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

    private static int[] componentBackgroundRGB;
    
    private static int[] pivotGroupLevelDarkenStepRGB;

    public static void init() {
        setCustomProperties(RootPanel.get().getElement(), getSelectedRowBackgroundColor(), getFocusedCellBackgroundColor(), getFocusedCellBorderColor(), getTableGridColor());
    }

    private static native void setCustomProperties(Element root, String selectedRowBackgroundColor, String focusedCellBackgroundColor, String focusedCellBorderColor, String tableGridColor) /*-{
        root.style.setProperty("--selected-row-background-color", selectedRowBackgroundColor);
        root.style.setProperty("--focused-cell-background-color", focusedCellBackgroundColor);
        root.style.setProperty("--focused-cell-border-color", focusedCellBorderColor);
        root.style.setProperty("--grid-separator-border-color", tableGridColor);
    }-*/;

    public static void reset() {
        componentBackgroundRGB = null;
        pivotGroupLevelDarkenStepRGB = null;

        init();
    }

    public static String getSelectedRowBackgroundColor() {
        if (MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getSelectedRowBackground();
            if (preferredColor != null) {
                return getThemedColor(preferredColor.toString());
            }
        }
        return null;
    }

    public static String getFocusedCellBackgroundColor() {
        if (MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBackground();
            if (preferredColor != null) {
                return getThemedColor(preferredColor.toString());
            }
        }
        return null;
    }

    public static String getFocusedCellBorderColor() {
        if (MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBorderColor();
            if (preferredColor != null) {
                return getThemedColor(preferredColor.toString());
            }
        }
        return null;
    }

    public static String getTableGridColor() {
        if (MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getTableGridColor();
            if(preferredColor != null) {
                return getThemedColor(preferredColor.toString());
            }
        }
        return null;
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
