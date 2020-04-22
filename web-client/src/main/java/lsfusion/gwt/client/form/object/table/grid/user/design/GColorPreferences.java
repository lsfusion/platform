package lsfusion.gwt.client.form.object.table.grid.user.design;

import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import java.io.Serializable;

public class GColorPreferences implements Serializable {
    private ColorDTO selectedRowBackground;
    private ColorDTO selectedCellBackground;
    private ColorDTO focusedCellBackground;
    private ColorDTO focusedCellBorderColor;


    @SuppressWarnings("unused")
    public GColorPreferences() {
    }

    public GColorPreferences(ColorDTO selectedRowBackground, ColorDTO selectedCellBackground,
                             ColorDTO focusedCellBackground, ColorDTO focusedCellBorderColor) {
        this.selectedRowBackground = selectedRowBackground;
        this.selectedCellBackground = selectedCellBackground;
        this.focusedCellBackground = focusedCellBackground;
        this.focusedCellBorderColor = focusedCellBorderColor;
    }

    public ColorDTO getSelectedRowBackground() {
        return selectedRowBackground;
    }

    public ColorDTO getSelectedCellBackground() {
        return selectedCellBackground;
    }

    public ColorDTO getFocusedCellBackground() {
        return focusedCellBackground;
    }

    public ColorDTO getFocusedCellBorderColor() {
        return focusedCellBorderColor;
    }
}
