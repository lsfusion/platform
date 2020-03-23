package lsfusion.client.base.view;

import lsfusion.client.controller.MainController;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static javax.swing.UIManager.getBorder;
import static javax.swing.UIManager.getColor;
import static lsfusion.client.controller.MainController.colorPreferences;

public class SwingDefaults {
    private static Color buttonBackground;
    private static Border buttonBorder;
    private static Color tableCellBackground;
    private static Color tableCellForeground;
    private static Border tableCellBorder;
    private static Color focusedTableCellBackground;
    private static Border focusedTableCellBorder;
    private static Color focusedTableRowBackground;
    private static Color focusedTableRowForeground;
    private static Color tableSelectionBackground;
    private static Color notDefinedForeground;
    
    public static void reset() {
        buttonBackground = null;
        buttonBorder = null;
        tableCellBackground = null;
        tableCellForeground = null;
        tableCellBorder = null;
        focusedTableCellBackground = null;
        focusedTableCellBorder = null;
        focusedTableRowBackground = null;
        focusedTableRowForeground = null;
        tableSelectionBackground = null;
        notDefinedForeground = null;
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

    public static Color getTableCellBackground() {
        if (tableCellBackground == null) {
            tableCellBackground = getColor("Table.background");
        }
        return tableCellBackground;
    }

    public static Color getTableCellForeground() {
        if (tableCellForeground == null) {
            tableCellForeground = getColor("Table.textForeground");
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
            Color preferredBackground = colorPreferences != null ? colorPreferences.getFocusedCellBackground() : null;
            focusedTableCellBackground = preferredBackground != null ? preferredBackground : getColor("Table.selectionBackground");
        }
        return focusedTableCellBackground;
    }

    public static Border getFocusedTableCellBorder() {
        if (focusedTableCellBorder == null) {
            Color preferredColor = colorPreferences != null ? colorPreferences.getFocusedCellBorderColor() : null;
            if (preferredColor != null) {
                focusedTableCellBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(preferredColor), BorderFactory.createEmptyBorder(1, 2, 1, 2)); 
            } else {
                focusedTableCellBorder = getBorder("Table.focusCellHighlightBorder");
            }
        }
        return focusedTableCellBorder; 
    }

    public static Color getFocusedTableRowBackground() {
        if (focusedTableRowBackground == null) {
            Color preferredBackground = colorPreferences != null ? colorPreferences.getSelectedRowBackground() : null;
            focusedTableRowBackground = preferredBackground != null ? preferredBackground : getColor("Table.selectionBackground");
        }
        return focusedTableRowBackground; 
    }

    public static Color getFocusedTableRowForeground() {
        if (focusedTableRowForeground == null) {
            focusedTableRowForeground = getColor("Table.selectionForeground");
        }
        return focusedTableRowForeground; 
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
            if (!MainController.colorTheme.isLight()) {
                notDefinedForeground = notDefinedForeground.darker(); // dark LAF returns the same color as enabled text field 
            }
        }
        return notDefinedForeground;
    }
}
