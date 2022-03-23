package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.HTMLLinkCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GHTMLLinkType extends GLinkType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeHTMLFileLinkCaption();
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new HTMLLinkCellRenderer(property);
    }
}