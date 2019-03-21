package lsfusion.gwt.client.form.property.cell.classes;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.shared.form.property.GPropertyDraw;

import static lsfusion.gwt.shared.GwtSharedUtils.isRedundantString;
import static lsfusion.gwt.shared.GwtSharedUtils.multiplyString;

public class StringGridCellRenderer extends TextBasedGridCellRenderer<String> {
    private boolean echoSymbols;
    private boolean isVarString;

    public StringGridCellRenderer(GPropertyDraw property, boolean isVarString) {
        super(property);
        this.isVarString = isVarString;
        echoSymbols = property.echoSymbols;
    }

    @Override
    protected String renderToString(String value) {
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

    @Override
    protected void updateElement(DivElement div, Object value) {
        super.updateElement(div, value == null ? null : value.toString());
    }
}
