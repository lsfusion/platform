package lsfusion.client.base.view;

import com.formdev.flatlaf.ui.FlatTableCellBorder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static javax.swing.UIManager.*;
import static lsfusion.client.controller.MainController.colorPreferences;
import static lsfusion.client.controller.MainController.colorTheme;
import static lsfusion.client.view.MainFrame.getIntUISize;

public class SwingDefaults {
    // no transparent colors as they are not drawn correctly sometimes.   
    final static Color selectionColorLight = new Color(211, 229, 232);
    final static Color selectionColorDark = new Color(44, 71, 81);
    final static Color focusedTableCellBorderColorLight = new Color(4, 137, 186);
    final static Color focusedTableCellBorderColorDark = new Color(7, 144, 195);
    
    private static Color buttonBackground;
    private static Border buttonBorder;
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
    private static Insets tableCellMargins;
    private static Insets buttonMargin;
    private static Integer valueHeight;
    private static Integer tableHeaderHeight;
    private static Dimension tablePreferredSize;
    private static Integer tableMaxPreferredHeight;
    private static Integer verticalToolbarNavigatorButtonHeight;
    
    public static void reset() {
        buttonBackground = null;
        buttonBorder = null;
        textFieldBorder = null;
        tableCellBackground = null;
        tableCellForeground = null;
        tableCellBorder = null;
        notDefinedForeground = null;
        logPanelErrorColor = null;
        titledBorderTitleColor = null;
        dockableBorderColor = null;
        tabbedPaneUnderlineColor = null;
        buttonMargin = null;
        
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
    
    public static Color getButtonBackground() {
        if (buttonBackground == null) {
            buttonBackground = getColor("Button.background");
        }
        return buttonBackground; 
    }

    public static Border getButtonBorder() {
        if (buttonBorder == null) {
            buttonBorder = getBorder("Button.border");
        }
        return buttonBorder;
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
            focusedTableCellBackground = colorPreferences != null ? colorPreferences.getFocusedCellBackground() : null;
            if (focusedTableCellBackground == null) {
                focusedTableCellBackground = getSelectionColor();
            }
        }
        return focusedTableCellBackground;
    }

    public static Border getFocusedTableCellBorder() {
        if (focusedTableCellBorder == null) {
            Color borderColor = colorPreferences != null ? colorPreferences.getFocusedCellBorderColor() : null;
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
            focusedTableRowBackground = colorPreferences != null ? colorPreferences.getSelectedRowBackground() : null;
            if (focusedTableRowBackground == null) {
                focusedTableRowBackground = getSelectionColor();
            }
        }
        return focusedTableRowBackground; 
    }

    public static Color getTableSelectionBackground() {
        if (tableSelectionBackground == null) {
            Color preferredBackground = colorPreferences != null ? colorPreferences.getSelectedCellBackground() : null;
            tableSelectionBackground = preferredBackground != null ? preferredBackground : getSelectionColor();
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
}
