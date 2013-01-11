package platform.gwt.form.shared.view.grid.renderer;

import platform.gwt.form.shared.view.GPropertyDraw;

public class StringGridCellRenderer extends TextBasedGridCellRenderer<String> {

    public StringGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String renderToString(String value) {
        return value == null ? null : value.trim();
    }
}
