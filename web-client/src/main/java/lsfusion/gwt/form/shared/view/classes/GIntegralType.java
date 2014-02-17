package lsfusion.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.NumberGridCellRenderer;

import java.text.ParseException;

public abstract class GIntegralType extends GDataType {
    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    protected Double parseToDouble(String s) throws ParseException {
        assert s != null;
        NumberFormat numberFormat = NumberFormat.getDecimalFormat();
        try {
            return numberFormat.parse(s);
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to double", 0);
        }
    }

    @Override
    public String getMinimumMask() {
        return "9 999 999";
    }

    @Override
    public String getPreferredMask() {
        return "99 999 999";
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}
