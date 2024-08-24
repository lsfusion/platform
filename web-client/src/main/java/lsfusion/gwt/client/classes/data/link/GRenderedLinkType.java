package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.link.ImageLinkCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public abstract class GRenderedLinkType extends GLinkType {

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new ImageLinkCellRenderer(property);
    }
}
