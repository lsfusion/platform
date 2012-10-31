package platform.gwt.form.shared.view.grid.renderer;

public class StringGridRenderer extends SafeHtmlGridRenderer<String> {

    @Override
    protected String renderToString(String value) {
        return value;
    }
}
