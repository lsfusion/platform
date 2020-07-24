package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.NumberCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public abstract class GIntegralType extends GFormatType<NumberFormat> {
    public final static String UNBREAKABLE_SPACE = "\u00a0";
    
    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberCellRenderer(property);
    }

    protected abstract int getPrecision();

    @Override
    public int getDefaultCharWidth() {
        int lengthValue = getPrecision();
        return Math.min(lengthValue <= 6 ? lengthValue : (int) round(6 + pow(lengthValue - 6, 0.7)), 10);
    }

    protected Double parseToDouble(String s) throws ParseException {
        assert s != null;
        try {
            String groupingSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().groupingSeparator();
            if (UNBREAKABLE_SPACE.equals(groupingSeparator)) {
                s = s.replace(" ", UNBREAKABLE_SPACE);
            }
            String decimalSeparator = LocaleInfo.getCurrentLocale().getNumberConstants().decimalSeparator();
            s = GwtClientUtils.replaceSeparators(s, decimalSeparator, groupingSeparator);
            return NumberFormat.getDecimalFormat().parse(s);
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to double", 0);
        }
    }

    public String formatDouble(Double value) {
        assert value != null;
        String s = NumberFormat.getDecimalFormat().format(value);
        return GwtClientUtils.replaceCommaSeparator(s.replace(UNBREAKABLE_SPACE, ""));
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }

    public NumberFormat getFormat(String pattern) {
        return NumberFormat.getDecimalFormat();
    }
}
