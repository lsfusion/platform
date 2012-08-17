package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.grid.renderer.ColorGridRenderer;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;

public class GColorType extends GDataType {
    public static GColorType instance = new GColorType();

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new ColorGridRenderer();
    }
}
