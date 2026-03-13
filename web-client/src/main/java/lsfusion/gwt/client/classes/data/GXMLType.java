package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.XMLCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GXMLType extends GStringFileBasedType {
    public static GXMLType instance = new GXMLType();

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new XMLCellRenderer(property);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeXMLCaption();
    }
}
