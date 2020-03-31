package lsfusion.client.base.view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static javax.swing.UIManager.*;
import static lsfusion.client.controller.MainController.colorPreferences;
import static lsfusion.client.controller.MainController.colorTheme;

public class SwingDefaults {
    final static Color focusedTableRowBackgroundLight = new Color(4, 137, 186, 23);
    final static Color focusedTableRowBackgroundDark = new Color(5, 114, 154, 61);
    
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
    private static Font textAreaFont;
    private static Color logPanelErrorColor;
    private static Color logPanelSuccessColor;
    private static Color titledBorderTitleColor;
    private static Color dockableBorderColor;
    
    public static void reset() {
        buttonBackground = null;
        buttonBorder = null;
        textFieldBorder = null;
        tableCellBackground = null;
        tableCellForeground = null;
        tableCellBorder = null;
        notDefinedForeground = null;
        textAreaFont = null;
        logPanelErrorColor = null;
        logPanelSuccessColor = null;
        titledBorderTitleColor = null;
        dockableBorderColor = null;
        
        resetTableSelectionProperties();
    }
    
    // properties are initialized before receiving color preferences
    public static void resetTableSelectionProperties() {
        focusedTableCellBackground = null;
        focusedTableCellBorder = null;
        focusedTableRowBackground = null;
        tableSelectionBackground = null;
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
                focusedTableCellBackground = colorTheme.isLight() ? focusedTableRowBackgroundLight : focusedTableRowBackgroundDark;
            }
        }
        return focusedTableCellBackground;
    }

    public static Border getFocusedTableCellBorder() {
        if (focusedTableCellBorder == null) {
            Color borderColor = colorPreferences != null ? colorPreferences.getFocusedCellBorderColor() : null;
            if (borderColor == null) {
                borderColor = colorTheme.isLight() ? new Color(4, 137, 186) : new Color(7, 144, 195);
            }
            focusedTableCellBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor),
                    BorderFactory.createEmptyBorder(1, 2, 1, 2));
        }
        return focusedTableCellBorder; 
    }

    public static Color getFocusedTableRowBackground() {
        if (focusedTableRowBackground == null) {
            focusedTableRowBackground = colorPreferences != null ? colorPreferences.getSelectedRowBackground() : null;
            if (focusedTableRowBackground == null) {
                focusedTableRowBackground = colorTheme.isLight() ? focusedTableRowBackgroundLight : focusedTableRowBackgroundDark;
            }
        }
        return focusedTableRowBackground; 
    }

    public static Color getTableSelectionBackground() {
        if (tableSelectionBackground == null) {
            Color preferredBackground = colorPreferences != null ? colorPreferences.getSelectedCellBackground() : null;
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

    public static Font getTextAreaFont() {
        if (textAreaFont == null) {
            textAreaFont = getFont("TextArea.font");
        }
        return textAreaFont;
    }
    
    public static Color getLogPanelErrorColor() {
        if (logPanelErrorColor == null) {
            logPanelErrorColor = colorTheme.isLight() ? new Color(255, 182, 182) : new Color(64, 0, 0);
        }
        return logPanelErrorColor;
    }
    
    public static Color getLogPanelSuccessColor() {
        if (logPanelSuccessColor == null) {
            // for some reason log panel refuses to draw ColorUIResource color
            logPanelSuccessColor = new Color(getColor("Component.focusColor").getRGB());
            if (!colorTheme.isLight()) {
                logPanelSuccessColor = logPanelSuccessColor.darker();
            }
        }
        return logPanelSuccessColor;
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
    
    public static Color getDockableBorderColor() {
        if (dockableBorderColor == null) {
            dockableBorderColor = getColor("Component.borderColor");
        }
        return dockableBorderColor;
    }
    
    
    // ----------- not cached properties ----------- //
    
    public static int getCellHeight() {
        return 19;
    } 
    
    public static int getSingleCellTableIntercellSpacing() {
        // to have right height and to be able to draw table border in editor
        return 2;
    }
}
