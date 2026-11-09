package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

// a full-text search type, TSVECTOR or TSQUERY: its value is shown as text and is not edited
public abstract class GTextSearchType extends GDataType {

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new TextCellRenderer(property);
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Full-text search classes don't support conversion from string", 0);
    }
}
