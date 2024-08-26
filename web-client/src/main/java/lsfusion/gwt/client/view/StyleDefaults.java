package lsfusion.gwt.client.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import java.util.HashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.view.ColorUtils.*;

public class StyleDefaults {
    public static int maxMobileWidthHeight = 570;

    public static final int CELL_HORIZONTAL_PADDING = 3;
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

        propertyValues.clear();

        init();
    }

    public static String getSelectedRowBackgroundColor() {
        if (MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getSelectedRowBackground();
            if (preferredColor != null) {
                return darkenColor(preferredColor.toString());
            }
        }
        return null;
    }

    public static String getFocusedCellBackgroundColor() {
        if (MainFrame.colorPreferences != null) { // might be called before colorPreferences initialization (color theme change)
            ColorDTO preferredColor = MainFrame.colorPreferences.getFocusedCellBackground();
            if (preferredColor != null) {
                return darkenColor(preferredColor.toString());
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
            int cbRGB = toRGB(getComponentBackground());
            componentBackgroundRGB = new int[]{getRed(cbRGB), getGreen(cbRGB), getBlue(cbRGB)};
        }
        return componentBackgroundRGB;
    }
    
    public static int[] getPivotGroupLevelDarkenStepRGB() {
        if (pivotGroupLevelDarkenStepRGB == null) {
            int pbRGB = toRGB(getPanelBackground());
            int[] panelRGB = new int[]{getRed(pbRGB), getGreen(pbRGB), getBlue(pbRGB)};
            int[] componentRGB = getComponentBackgroundRGB();
            
            float darkenStep = 0.8f;

            pivotGroupLevelDarkenStepRGB = new int[]{
                    Math.min((int) ((panelRGB[0] - componentRGB[0]) * darkenStep), -10),
                    Math.min((int) ((panelRGB[1] - componentRGB[1]) * darkenStep), -10),
                    Math.min((int) ((panelRGB[2] - componentRGB[2]) * darkenStep), -10),
            };
        }
        return pivotGroupLevelDarkenStepRGB;
    }

    // We assume that the colours are initially set by the developers in the light theme, and we need white as the base colour for the calculation.
    public static String getDefaultComponentBackground() {
        return "#FFFFFF";
    }

    private static final Map<String, String> propertyValues = new HashMap<>();
    private static String getCachedPropertyValue(String property) {
        String propertyValue = propertyValues.get(property);
        if (propertyValue == null) {
            propertyValue = getPropertyValue(Document.get().getDocumentElement(), property);
            propertyValues.put(property, propertyValue.trim()); //trim because getPropertyValue() returns a value with a random space at beginning or end of value and then ColorUtils.toRGB returns invalid value
        }
        return propertyValue;
    }

    //pass the element because otherwise there is no access to the valid .js-document element
    private static native String getPropertyValue(Element element, String property) /*-{
        return getComputedStyle(element).getPropertyValue(property);
    }-*/;

    public static String getComponentBackground() {
        return getCachedPropertyValue("--component-background-color");
    }

    public static String getPanelBackground() {
        return getCachedPropertyValue("--background-color");
    }

    public static String getTextColor() {
        return getCachedPropertyValue("--text-color");
    }

    public static String getGridSeparatorBorderColor() {
        return getCachedPropertyValue("--grid-separator-border-color");
    }
}
