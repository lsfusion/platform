package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

public abstract class SafeHtmlGridRenderer<T> implements GridCellRenderer {
    interface Template extends SafeHtmlTemplates {
        @Template("<div style=\"text-align: {0};\">{1}</div>")
        SafeHtml aligned(String alignment, SafeHtml text);
    }

    private static Template template;

    protected final SafeHtmlRenderer<String> renderer;
    protected final Style.TextAlign textAlign;

    public SafeHtmlGridRenderer() {
        this(null);
    }

    public SafeHtmlGridRenderer(Style.TextAlign textAlign) {
        this(SimpleSafeHtmlRenderer.getInstance(), textAlign);
    }

    public SafeHtmlGridRenderer(SimpleSafeHtmlRenderer renderer, Style.TextAlign textAlign) {
        if (template == null) {
            template = GWT.create(Template.class);
        }

        this.renderer = renderer;
        this.textAlign = textAlign == Style.TextAlign.LEFT ? null : textAlign;
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        String sValue = value == null ? null : renderToString((T) value);
        renderAligned(sValue, renderer, textAlign, sb);
    }

    protected abstract String renderToString(T value);

    public static void renderAligned(String text, SafeHtmlRenderer<String> renderer, Style.TextAlign textAlign, SafeHtmlBuilder sb) {
        if (text == null || text.trim().isEmpty()) {
            sb.appendHtmlConstant("&nbsp;");
        } else {
            SafeHtml safeText = renderer.render(text);

            if (textAlign != null) {
                safeText = template.aligned(textAlign.getCssName(), safeText);
            }

            sb.append(safeText);
        }
    }
}
