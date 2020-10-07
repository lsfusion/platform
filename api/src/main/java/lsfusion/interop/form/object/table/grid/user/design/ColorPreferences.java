package lsfusion.interop.form.object.table.grid.user.design;

import java.awt.*;
import java.io.Serializable;

public class ColorPreferences implements Serializable {
    private Color selectedRowBackground;
    private Color selectedCellBackground;
    private Color focusedCellBackground;
    private Color focusedCellBorderColor;
    private Color tableGridColor;


    public ColorPreferences(Color selectedRowBackground, Color selectedCellBackground, 
                            Color focusedCellBackground, Color focusedCellBorderColor,
                            Color tableGridColor) {
        this.selectedRowBackground = selectedRowBackground;
        this.selectedCellBackground = selectedCellBackground;
        this.focusedCellBackground = focusedCellBackground;
        this.focusedCellBorderColor = focusedCellBorderColor;
        this.tableGridColor = tableGridColor;
    }

    public Color getSelectedRowBackground() {
        return selectedRowBackground;
    }

    public Color getSelectedCellBackground() {
        return selectedCellBackground;
    }
    
    public Color getFocusedCellBackground() {
        return focusedCellBackground;
    }
    
    public Color getFocusedCellBorderColor() {
        return focusedCellBorderColor;
    }
    
    public Color getTableGridColor() {
        return tableGridColor;
    }
}