package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.base.GwtSharedUtils.multiplyString;

public class StringCellRenderer extends StringBasedCellRenderer<String> {
    private boolean echoSymbols;
    private boolean isVarString;

    public StringCellRenderer(GPropertyDraw property, boolean isVarString) {
        super(property);
        this.isVarString = isVarString;
        echoSymbols = property.echoSymbols;
    }

    @Override
    public String format(String value) {
        if (echoSymbols) {
            return multiplyString(EscapeUtils.UNICODE_BULLET, 6);
        } else if (!isVarString) {
            return value.trim();
        } else {
            return value;
        }
    }
}
