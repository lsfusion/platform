package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.link.ImageLinkCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GVideoLinkType extends GRenderedLinkType {
    @Override
    public String getExtension() {
        return "mp4";
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeVideoFileLinkCaption();
    }
}