package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

public abstract class SafeHtmlGridRenderer<T> implements GridCellRenderer {

    protected final SafeHtmlRenderer<String> renderer;

    public SafeHtmlGridRenderer() {
        this(SimpleSafeHtmlRenderer.getInstance());
    }

    public SafeHtmlGridRenderer(SimpleSafeHtmlRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        String sValue = null;
        if (value != null) {
            sValue = renderToString((T) value);
        }
        if (sValue == null || sValue.trim().isEmpty()) {
            sb.appendHtmlConstant("&nbsp;");
        } else {
            sb.append(renderer.render(sValue));
        }
    }

    protected abstract String renderToString(T value);
}
