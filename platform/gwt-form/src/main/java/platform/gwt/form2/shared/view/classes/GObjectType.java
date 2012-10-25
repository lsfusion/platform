package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form2.shared.view.grid.renderer.NumberGridRenderer;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridRenderer();
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return 50;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return 50;
    }
}
