package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.cell.GridCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.ImageGridCellRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class GImageType extends GFileType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ImageGridCellRenderer(property);
    }
    
    public String extension;

    public GImageType() {
    }

    public GImageType(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeImageCaption();
    }
}
