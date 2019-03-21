package lsfusion.gwt.shared.classes.data.link;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.ui.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.ui.grid.renderer.HTMLGridCellRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class GHTMLLinkType extends GLinkType {
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeHTMLFileLinkCaption();
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new HTMLGridCellRenderer();
    }
}