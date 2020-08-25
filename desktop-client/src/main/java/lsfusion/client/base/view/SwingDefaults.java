package lsfusion.client.base.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatTableCellBorder;
import lsfusion.interop.base.view.ColorTheme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static lsfusion.client.base.view.ClientColorUtils.getDisplayColor;
import static lsfusion.client.controller.MainController.colorPreferences;
import static lsfusion.client.controller.MainController.colorTheme;
import static lsfusion.client.view.MainFrame.getIntUISize;
import static lsfusion.interop.base.view.ColorTheme.DEFAULT;

public class SwingDefaults {
    private static final UIDefaults lightDefaults = new FlatLightLaf().getDefaults();
    private static final UIDefaults darkDefaults = new FlatDarkLaf().getDefaults();

    // no transparent colors as they are not drawn correctly sometimes.   
    private static final Color selectionColorLight = new Color(211, 229, 232);
    private static final Color selectionColorDark = new Color(44, 71, 81);
    private static final Color focusedTableCellBorderColorLight = new Color(4, 137, 186);
    private static final Color focusedTableCellBorderColorDark = new Color(7, 144, 195);

    private static Color defaultThemeTableCellBackground;
    private static Color defaultThemePanelBackground;

    private static Color componentFocusBorderColor;
    private static Color buttonBackground;
    private static Color buttonForeground;
    private static Border buttonBorder;
    private static Color buttonHoverBackground;
    private static Color buttonPressedBackground;
    private static Color toggleButtonHoverBackground;
    private static Color toggleButtonPressedBackground;
    private static Border textFieldBorder;
    private static Color tableCellBackground;
    private static Color tableCellForeground;
    private static Border tableCellBorder;
    private static Color focusedTableCellBackground;
    private static Border focusedTableCellBorder;
    private static Color focusedTableRowBackground;
    private static Color tableSelectionBackground;
    private static Color notDefinedForeground;
    private static Color logPanelErrorColor;
    private static Color titledBorderTitleColor;
    private static Color dockableBorderColor;
    private static Color tabbedPaneUnderlineColor;
    private static Color tabbedPaneFocusColor;
    private static Color notNullCornerTriangleColor;
    private static Color notNullLineColor;
    private static Color hasChangeActionColor;
    private static Color requiredForeground;
    private static Color validDateForeground;
    private static Insets tableCellMargins;
    private static Insets buttonMargin;
    private static Insets toggleButtonMargin;
    private static Integer valueHeight;
    private static Integer tableHeaderHeight;
    private static Dimension tablePreferredSize;
    private static Integer tableMaxPreferredHeight;
    private static Integer verticalToolbarNavigatorButtonHeight;
    private static Color panelBackground;

    public static void reset() {
        componentFocusBorderColor = null;
        buttonBackground = null;
        buttonForeground = null;
        buttonBorder = null;
        buttonHoverBackground = null;
        buttonPressedBackground = null;
        toggleButtonHoverBackground = null;
        toggleButtonPressedBackground = null;
        textFieldBorder = null;
        tableCellBackground = null;
        tableCellForeground = null;
        tableCellBorder = null;
        notDefinedForeground = null;
        logPanelErrorColor = null;
        titledBorderTitleColor = null;
        dockableBorderColor = null;
        tabbedPaneUnderlineColor = null;
        tabbedPaneFocusColor = null;
        notNullCornerTriangleColor = null;
        notNullLineColor = null;
        hasChangeActionColor = null;
        requiredForeground = null;
        validDateForeground = null;
        buttonMargin = null;
        toggleButtonMargin = null;
        panelBackground = null;
        
        resetClientSettingsProperties();
    }
    
    // properties are initialized before receiving client settings
    // everything that might be changed by fontSize or colorPreferences
    public static void resetClientSettingsProperties() {
        focusedTableCellBackground = null;
        focusedTableCellBorder = null;
        focusedTableRowBackground = null;
        tableSelectionBackground = null;
        valueHeight = null;
        tableCellMargins = null;
        tableHeaderHeight = null;
        tablePreferredSize = null;
        tableMaxPreferredHeight = null;
        verticalToolbarNavigatorButtonHeight = null;
    }
    
    public static Object get(String key) {
        return get(key, colorTheme);
    }

    public static Object get(String key, ColorTheme theme) {
        return (theme.isLight() ? lightDefaults : darkDefaults).get(key);
    }
    
    public static Color getColor(String key) {
        return (Color) get(key);
    }
    
    public static Color getColor(String key, ColorTheme theme) {
        return (Color) get(key, theme);
    }

    public static Border getBorder(String key) {
        return (Border) get(key);
    }
    
    public static Insets getInsets(String key) {
        return (Insets) get(key);
    }
    
    public static Color getComponentFocusBorderColor() {
        if (componentFocusBorderColor == null) {
            componentFocusBorderColor = colorTheme.isLight() ? new Color(132, 192, 214) : new Color(70, 127, 147);
        }
        return componentFocusBorderColor; 
    }

    public static Color getButtonBackground() {
        if (buttonBackground == null) {
            buttonBackground = getColor("Button.background");
        }
        return buttonBackground; 
    }

    public static Color getButtonForeground() {
        if (buttonForeground == null) {
            buttonForeground = getColor("Button.foreground");
        }
        return buttonForeground;
    }

    public static Border getButtonBorder() {
        if (buttonBorder == null) {
            buttonBorder = getBorder("Button.border");
        }
        return buttonBorder;
    } 

    public static Color getButtonHoverBackground() {
        if (buttonHoverBackground == null) {
            buttonHoverBackground = colorTheme.isLight() ? new Color(247, 247, 247) : new Color(83, 88, 90);
        }
        return buttonHoverBackground;
    }

    public static Color getButtonPressedBackground() {
        if (buttonPressedBackground == null) {
            buttonPressedBackground = colorTheme.isLight() ? new Color(230, 230, 230) : new Color(91, 95, 98);
        }
        return buttonPressedBackground;
    }

    public static Color getToggleButtonHoverBackground() {
        if (toggleButtonHoverBackground == null) {
            toggleButtonHoverBackground = colorTheme.isLight() ? new Color(230, 230, 230) : new Color(70, 73, 75);
        }
        return toggleButtonHoverBackground;
    }

    public static Color getToggleButtonPressedBackground() {
        if (toggleButtonPressedBackground == null) {
            toggleButtonPressedBackground = colorTheme.isLight() ? new Color(222, 222, 222) : new Color(80, 83, 85);
        }
        return toggleButtonPressedBackground;
    }

    public static Border getTextFieldBorder() {
        if (textFieldBorder == null) {
            textFieldBorder = getBorder("TextField.border");
        }
        return textFieldBorder;
    } 

    public static Color getTableCellBackground() {
        if (tableCellBackground == null) {
            tableCellBackground = getColor("Table.background");
        }
        return tableCellBackground;
    }

    public static Color getTableCellForeground() {
        if (tableCellForeground == null) {
            tableCellForeground = getColor("Table.foreground");
        }
        return tableCellForeground;
    }

    public static Border getTableCellBorder() {
        if (tableCellBorder == null) {
            tableCellBorder = getBorder("Table.cellNoFocusBorder");
        }
        return tableCellBorder;
    }

    public static Color getFocusedTableCellBackground() {
        if (focusedTableCellBackground == null) {
            focusedTableCellBackground = getDisplayColor(colorPreferences != null ? colorPreferences.getFocusedCellBackground() : null);
            if (focusedTableCellBackground == null) {
                focusedTableCellBackground = getSelectionColor();
            }
        }
        return focusedTableCellBackground;
    }

    public static Border getFocusedTableCellBorder() {
        if (focusedTableCellBorder == null) {
            Color borderColor = getDisplayColor(colorPreferences != null ? colorPreferences.getFocusedCellBorderColor() : null);
            if (borderColor == null) {
                borderColor = getSelectionBorderColor();
            }
            Insets insets = getTableCellMargins();
            focusedTableCellBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor),
                    BorderFactory.createEmptyBorder(insets.top - 1, insets.left - 1, insets.bottom - 1, insets.right - 1));
        }
        return focusedTableCellBorder; 
    }

    public static Color getFocusedTableRowBackground() {
        if (focusedTableRowBackground == null) {
            focusedTableRowBackground = getDisplayColor(colorPreferences != null ? colorPreferences.getSelectedRowBackground() : null);
            if (focusedTableRowBackground == null) {
                focusedTableRowBackground = getSelectionColor();
            }
        }
        return focusedTableRowBackground; 
    }

    public static Color getTableSelectionBackground() {
        if (tableSelectionBackground == null) {
            Color preferredBackground = getDisplayColor(colorPreferences != null ? colorPreferences.getSelectedCellBackground() : null);
            tableSelectionBackground = preferredBackground != null ? preferredBackground : getColor("Table.selectionInactiveBackground");
        }
        return tableSelectionBackground; 
    }

    public static Color getNotDefinedForeground() {
        if (notDefinedForeground == null) {
            notDefinedForeground = getColor("TextField.inactiveForeground");
            if (!colorTheme.isLight()) {
                notDefinedForeground = notDefinedForeground.darker(); // dark LAF returns the same color as enabled text field 
            }
        }
        return notDefinedForeground;
    }
    
    public static Color getLogPanelErrorColor() {
        if (logPanelErrorColor == null) {
            logPanelErrorColor = colorTheme.isLight() ? new Color(255, 182, 182) : new Color(64, 0, 0);
        }
        return logPanelErrorColor;
    }

    public static Color getTitledBorderTitleColor() {
        if (titledBorderTitleColor == null) {
            // Trying to be close to default titled border color.
            // As we don't know what TitledBorder.border contains, use Separator.foreground (as FlatLaf does).
            Color borderColor = getColor("Separator.foreground");
            if (colorTheme.isLight()) {
                titledBorderTitleColor = borderColor.darker();
            } else {
                titledBorderTitleColor = borderColor.brighter();
            }
        }
        return titledBorderTitleColor;
    }
    
    public static Color getComponentBorderColor() {
        if (dockableBorderColor == null) {
            dockableBorderColor = getColor("Component.borderColor");
        }
        return dockableBorderColor;
    }
    
    public static Color getTabbedPaneUnderlineColor() {
        if (tabbedPaneUnderlineColor == null) {
            tabbedPaneUnderlineColor = colorTheme.isLight() ? new Color(177 , 220, 226) : new Color(41, 82, 96);
        }
        return tabbedPaneUnderlineColor;
    }
    
    public static Color getTabbedPaneFocusColor() {
        if (tabbedPaneFocusColor == null) {
            tabbedPaneFocusColor = colorTheme.isLight() ? new Color(215, 232, 234) : new Color(61, 83, 91);
        }
        return tabbedPaneFocusColor;
    }

    public static Color getNotNullCornerTriangleColor() {
        if (notNullCornerTriangleColor == null) {
            notNullCornerTriangleColor = colorTheme.isLight() ? new Color(255, 0, 0) : new Color(195, 0, 0);
        }
        return notNullCornerTriangleColor;
    }

    public static Color getNotNullLineColor() {
        if (notNullLineColor == null) {
            notNullLineColor = colorTheme.isLight() ? new Color(255, 184, 184) : new Color(154, 0, 0);
        }
        return notNullLineColor;
    }

    public static Color getHasChangeActionColor() {
        if (hasChangeActionColor == null) {
            hasChangeActionColor = colorTheme.isLight() ? focusedTableCellBorderColorLight : focusedTableCellBorderColorDark;
        }
        return hasChangeActionColor;
    }

    public static Color getRequiredForeground() {
        if (requiredForeground == null) {
            requiredForeground = colorTheme.isLight() ? new Color(199, 13, 0) : new Color(197, 77, 77);
        }
        return requiredForeground;
    }
    
    public static Color getValidDateForeground() {
        if (validDateForeground == null) {
            validDateForeground = colorTheme.isLight() ? new Color(0, 150, 0) : new Color(0, 130, 0);
        }
        return validDateForeground;
    }
    
    public static Insets getTableCellMargins() {
        if (tableCellMargins == null) {
            Border border = getTableCellBorder();
            if (border instanceof FlatTableCellBorder) {
                tableCellMargins = ((FlatTableCellBorder) border).getBorderInsets(); // increases when fontSize is increased.
            } else {
                tableCellMargins = getInsets("Table.cellMargins"); // fallback property to avoid null checks. this value is permanent.
            }
        }
        return tableCellMargins;
    }
    
    public static Insets getButtonMargin() {
        if (buttonMargin == null) {
            buttonMargin = getInsets("Button.margin");
        }
        return buttonMargin;
    }
    
    public static Insets getToggleButtonMargin() { // for toolbar navigator
        if (toggleButtonMargin == null) {
            toggleButtonMargin = new Insets(2, 7, 2, 7);
        }
        return toggleButtonMargin;
    }
    
    public static int getValueHeight() {
        if (valueHeight == null) {
            valueHeight = getIntUISize(20);
        }
        return valueHeight;
    }
    
    public static int getTableHeaderHeight() {
        if (tableHeaderHeight == null) {
            tableHeaderHeight = getIntUISize(34);
        }
        return tableHeaderHeight;
    } 

    public static Dimension getTablePreferredSize() {
        if (tablePreferredSize == null) {
            tablePreferredSize = new Dimension(getIntUISize(130), getIntUISize(70) - getTableHeaderHeight());
        }
        return tablePreferredSize;
    } 

    public static int getTableMaxPreferredHeight() {
        if (tableMaxPreferredHeight == null) {
            tableMaxPreferredHeight = getIntUISize(70);
        }
        return tableMaxPreferredHeight;
    } 

    public static int getVerticalToolbarNavigatorButtonHeight() {
        if (verticalToolbarNavigatorButtonHeight == null) {
            verticalToolbarNavigatorButtonHeight = getIntUISize(30);
        }
        return verticalToolbarNavigatorButtonHeight;
    } 

    public static Color getPanelBackground() {
        if (panelBackground == null) {
            panelBackground = getColor("Panel.background");
        }
        return panelBackground;
    }


    public static Color getDefaultThemeTableCellBackground() {
        if (defaultThemeTableCellBackground == null) {
            defaultThemeTableCellBackground = getColor("Table.background", DEFAULT);
        }
        return defaultThemeTableCellBackground;
    }

    public static Color getDefaultThemePanelBackground() {
        if (defaultThemePanelBackground == null) {
            defaultThemePanelBackground = getColor("Panel.background", DEFAULT);
        }
        return defaultThemePanelBackground;
    }

    
    // ----------- not cached properties ----------- //
    
    
    public static int getButtonBorderWidth() {
        return 1;
    }
    
    public static int getComponentHeight() {
        return getValueHeight() + 2; // supposing all components have border width = 1px
    }
    
    public static int getSingleCellTableIntercellSpacing() {
        // to have right height and to be able to draw table border in editor
        return 2;
    }
    
    public static Color getSelectionColor() {
        return colorTheme.isLight() ? selectionColorLight : selectionColorDark;
    }
    
    public static Color getSelectionBorderColor() {
        return colorTheme.isLight() ? focusedTableCellBorderColorLight : focusedTableCellBorderColorDark;
    }
    
    public static int splitDividerWidth() {
        return 6;
    }
}
