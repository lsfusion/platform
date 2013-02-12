package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.ImageGridCellRenderer;

public class GImageType extends GFileType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageGridCellRenderer();
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        return Integer.MAX_VALUE;
    }
}
