package lsfusion.interop.form;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.Serializable;

public class ColorPreferences implements Serializable {
    private Color selectedRowBackground;
    private Border selectedRowBorder;
    private Color selectedCellBackground;
    private Color focusedCellBackground;
    private Border focusedCellBorder;


    public ColorPreferences(Color selectedRowBackground, Color selectedRowBorder, Color selectedCellBackground, 
                            Color focusedCellBackground, Color focusedCellBorder) {
        this.selectedRowBackground = selectedRowBackground;
        this.selectedRowBorder = BorderFactory.createMatteBorder(1, 0, 1, 0, selectedRowBorder);
        this.selectedCellBackground = selectedCellBackground;
        this.focusedCellBackground = focusedCellBackground;
        this.focusedCellBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, focusedCellBorder);
    }

    public Color getSelectedRowBackground() {
        return selectedRowBackground;
    }

    public Border getSelectedRowBorder() {
        return selectedRowBorder;
    }

    public Color getSelectedCellBackground() {
        return selectedCellBackground;
    }
    
    public Color getFocusedCellBackground() {
        return focusedCellBackground;
    }
    
    public Border getFocusedCellBorder() {
        return focusedCellBorder;
    }
}