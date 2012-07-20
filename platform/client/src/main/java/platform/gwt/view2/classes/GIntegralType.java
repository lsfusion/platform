package platform.gwt.view2.classes;

import platform.gwt.view2.grid.renderer.GridCellRenderer;
import platform.gwt.view2.grid.renderer.NumberGridRenderer;

public abstract class GIntegralType extends GDataType {
    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new NumberGridRenderer();
    }
}
