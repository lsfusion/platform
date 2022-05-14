package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.IntegralCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public abstract class GIntegralType extends GFormatType {
    public final static String UNBREAKABLE_SPACE = "\u00a0";
    
    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new IntegralCellRenderer(property);
    }

    protected abstract int getPrecision();

    @Override
    public int getDefaultCharWidth() {
        int lengthValue = getPrecision();
        return Math.min(lengthValue <= 6 ? lengthValue : (int) round(6 + pow(lengthValue - 6, 0.7)), 10);
    }


    // paste, edit
    @Override
    public Object parseString(String s, String pattern, boolean edit) throws ParseException {
        if(s.isEmpty())
            return null;

        try {
            return convertDouble(getFormat(pattern).parse(GwtClientUtils.editParse(s)));
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to double", 0);
        }
    }

    public abstract Object convertDouble(Double doubleValue);

    // render, edit
    @Override
    public String formatString(Object value, String pattern, boolean edit) {
        if(value == null)
            return null;

        // there was doubleValue before, but not sure what for
        String result = getFormat(pattern).format((Number) value);
        if(edit)
            result = GwtClientUtils.editFormat(result);
        return result;
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }

    protected NumberFormat getFormat(String pattern) {
        return pattern != null ? NumberFormat.getFormat(pattern) : getDefaultFormat();
    }

    protected NumberFormat getDefaultFormat() {
        return NumberFormat.getDecimalFormat();
    }
}
