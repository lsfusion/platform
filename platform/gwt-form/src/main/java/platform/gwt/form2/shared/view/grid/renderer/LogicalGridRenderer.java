package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;

public class LogicalGridRenderer implements GridCellRenderer {
    interface Template extends SafeHtmlTemplates {
        @Template("<center><img src=\"{0}\"/><center>")
        SafeHtml checkbox(SafeUri url);
    }

    private static Template template;

    public LogicalGridRenderer() {
        if (template == null) {
            template = GWT.create(Template.class);
        }
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        boolean checked = value != null && (Boolean) value;
        String cbImagePath = GWT.getModuleBaseURL() + "images/checkbox_" + (checked ? "checked" : "unchecked") + ".png";
        sb.append(template.checkbox(UriUtils.fromString(cbImagePath)));
    }
}
