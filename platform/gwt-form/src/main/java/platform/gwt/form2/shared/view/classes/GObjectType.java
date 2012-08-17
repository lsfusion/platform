package platform.gwt.form2.shared.view.classes;

import platform.gwt.form2.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form2.shared.view.grid.renderer.NumberGridRenderer;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public GridCellRenderer createGridCellRenderer() {
        return new NumberGridRenderer();
    }
}
