package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.IntegralCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.view.MainFrame;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public abstract class GIntegralType extends GFormatType {
    public final static String UNBREAKABLE_SPACE = "\u00a0";
    
    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new IntegralCellRenderer(property);
    }

    protected abstract int getPrecision();

    public String getStep() {
        return GwtClientUtils.getStep(getPrecision());
    }

    @Override
    public int getDefaultCharWidth() {
        int lengthValue = getPrecision();
        return Math.min(lengthValue <= 6 ? lengthValue : (int) round(6 + pow(lengthValue - 6, 0.7)), 10);
    }


    // paste, edit
    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        if(s.isEmpty())
            return null;

        try {
            return convertDouble(getFormat(pattern).parse(GwtClientUtils.editParse(s)));
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to double", 0);
        }
    }

    public abstract PValue convertDouble(Double doubleValue);

    // render, edit
    @Override
    public String formatString(PValue value, String pattern) {
        // there was doubleValue before, but not sure what for
        return getFormat(pattern).format(PValue.getNumberValue(value));
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

    private final static GInputType inputType = new GInputType("number");
    @Override
    public GInputType getValueInputType() {
        if(MainFrame.mobile)
            return inputType;
        return super.getValueInputType();
    }
}
