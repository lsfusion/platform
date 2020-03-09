package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.link.ImageLinkGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;

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