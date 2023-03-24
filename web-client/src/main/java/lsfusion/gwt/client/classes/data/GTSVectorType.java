package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

public class GTSVectorType extends GDataType {
    public static GTSVectorType instance = new GTSVectorType();

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new TextCellRenderer(property);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        return GSize.CONST(150);
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("TSVector class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTSVectorCaption();
    }
}
