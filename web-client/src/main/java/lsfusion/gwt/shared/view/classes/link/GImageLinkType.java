package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.ui.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.ui.grid.renderer.ImageLinkGridCellRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class GImageLinkType extends GLinkType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageLinkGridCellRenderer(property);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeImageLinkCaption();
    }
}