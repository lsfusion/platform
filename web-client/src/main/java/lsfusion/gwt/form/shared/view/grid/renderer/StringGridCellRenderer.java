package lsfusion.gwt.form.shared.view.grid.renderer;

import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

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
            return GwtSharedUtils.multiplyString(EscapeUtils.UNICODE_BULLET, 6);
        } else if (!isVarString) {
            return value.trim();
        } else {
            return value;
        }
    }
}
