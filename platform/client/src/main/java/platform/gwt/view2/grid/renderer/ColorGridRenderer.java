package platform.gwt.view2.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;

public class ColorGridRenderer implements GridCellRenderer {
    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        SafeHtml safeColor = SimpleSafeHtmlRenderer.getInstance().render((String) value);
        sb.appendHtmlConstant("<div style=\"border: 1px solid black; background: " + safeColor.asString() + ";color: " + safeColor.asString() + ";\">&nbsp</div>");
    }
}
