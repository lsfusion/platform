package lsfusion.gwt.form.shared.view.classes.link;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.ImageLinkGridCellRenderer;

public class GImageLinkType extends GLinkType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageLinkGridCellRenderer(property);
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font, String pattern) {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "Ссылка на файл картинки";
    }
}