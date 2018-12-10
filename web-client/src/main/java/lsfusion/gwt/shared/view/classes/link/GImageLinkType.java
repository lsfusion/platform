package lsfusion.gwt.shared.view.classes.link;

import lsfusion.gwt.client.MainFrameMessages;
import lsfusion.gwt.client.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.grid.renderer.ImageLinkGridCellRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class GImageLinkType extends GLinkType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageLinkGridCellRenderer(property);
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeImageLinkCaption();
    }
}