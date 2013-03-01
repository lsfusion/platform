package platform.gwt.form.shared.view.grid.renderer;

import platform.gwt.base.client.EscapeUtils;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form.shared.view.GPropertyDraw;

public class StringGridCellRenderer extends TextBasedGridCellRenderer<String> {
    private boolean echoSymbols;

    public StringGridCellRenderer(GPropertyDraw property) {
        super(property);
        echoSymbols = property.echoSymbols;
    }

    @Override
    protected String renderToString(String value) {
        return echoSymbols ? GwtClientUtils.multiplyString(EscapeUtils.UNICODE_BULLET, 6) : value.trim();
    }
}
