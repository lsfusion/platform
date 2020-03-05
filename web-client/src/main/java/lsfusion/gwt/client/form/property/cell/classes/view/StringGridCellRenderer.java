package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;
import static lsfusion.gwt.client.base.GwtSharedUtils.multiplyString;

public class StringGridCellRenderer extends StringBasedGridCellRenderer<String> {
    private boolean echoSymbols;
    private boolean isVarString;

    public StringGridCellRenderer(GPropertyDraw property, boolean isVarString) {
        super(property);
        this.isVarString = isVarString;
        echoSymbols = property.echoSymbols;
    }

    @Override
    protected String castToString(String value) {
        if (echoSymbols) {
            return multiplyString(EscapeUtils.UNICODE_BULLET, 6);
        } else if (!isVarString) {
            if (isRedundantString(value)) {
                return null;
            }
            return value.trim();
        } else {
            return value;
        }
    }
}
