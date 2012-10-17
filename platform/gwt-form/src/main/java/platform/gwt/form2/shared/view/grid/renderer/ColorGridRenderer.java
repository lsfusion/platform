package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ColorGridRenderer implements GridCellRenderer {
    public interface Template extends SafeHtmlTemplates {
        @Template("<div style=\"height: 16px; border: 0px solid black; background: {0}; color: {0};\">&nbsp</div>")
        SafeHtml colorbox(String color);

        public static final class Instance {
            private static final ColorGridRenderer.Template template = GWT.create(ColorGridRenderer.Template.class);

            public static ColorGridRenderer.Template get() {
                return template;
            }

            public static SafeHtml colorbox(Object value) {
                return get().colorbox(value == null ? "" : value.toString());
            }
        }
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        sb.append(Template.Instance.colorbox(value));
    }
}
