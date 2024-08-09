package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GPDFType extends GFileType {
    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new ImageCellRenderer(property);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typePDFFileCaption();
    }
}
