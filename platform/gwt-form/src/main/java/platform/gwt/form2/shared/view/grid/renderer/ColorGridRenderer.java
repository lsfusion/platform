package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

public class ColorGridRenderer implements GridCellRenderer {
    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        String color = value == null ? null : value.toString();
        SafeHtml safeColor = SimpleSafeHtmlRenderer.getInstance().render(color);
        sb.appendHtmlConstant("<div style=\"border: 0px solid black; background: " + safeColor.asString() + ";color: " + safeColor.asString() + ";\">&nbsp</div>");
    }
}
