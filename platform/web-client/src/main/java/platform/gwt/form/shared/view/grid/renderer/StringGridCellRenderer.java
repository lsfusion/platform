package platform.gwt.form.shared.view.grid.renderer;

public class StringGridCellRenderer extends TextGridCellRenderer<String> {

    @Override
    protected String renderToString(String value) {
        return value == null ? null : value.trim();
    }
}
