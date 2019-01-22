package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.ui.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.ui.grid.renderer.ImageGridCellRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class GImageType extends GFileType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageGridCellRenderer(property);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeImageCaption();
    }
}
