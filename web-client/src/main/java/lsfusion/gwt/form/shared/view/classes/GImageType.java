package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.ImageGridCellRenderer;

public class GImageType extends GFileType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageGridCellRenderer(property);
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "Файл картинки";
    }
}
