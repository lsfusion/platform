package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form2.shared.view.grid.renderer.NumberGridRenderer;

public abstract class GIntegralType extends GDataType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridRenderer();
    }

    @Override
    public String getMinimumMask() {
        return "9 999 999";
    }

    @Override
    public String getPreferredMask() {
        return "99 999 999";
    }
}
