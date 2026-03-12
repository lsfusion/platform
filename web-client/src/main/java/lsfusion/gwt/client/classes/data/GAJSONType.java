package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.JSONCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public abstract class GAJSONType extends GStringFileBasedType {

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new JSONCellRenderer(property);
    }
}
