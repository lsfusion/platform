package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form2.shared.view.grid.renderer.NumberGridRenderer;

public abstract class GIntegralType extends GDataType {
    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new NumberGridRenderer();
    }
}
