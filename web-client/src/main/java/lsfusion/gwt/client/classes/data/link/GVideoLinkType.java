package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.FrameLinkCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GVideoLinkType extends GLinkType {
    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new FrameLinkCellRenderer(property);
    }
    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeVideoFileLinkCaption();
    }
}