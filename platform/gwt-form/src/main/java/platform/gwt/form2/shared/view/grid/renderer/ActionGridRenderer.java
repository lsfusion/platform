package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ActionGridRenderer implements GridCellRenderer {
    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        boolean disabled = value == null || !(Boolean) value;
        String constant = "<button type=\"button\" tabindex=\"-1\" value=\"...\"";
        if (disabled) {
            constant += " disabled=\"disabled\"";
        }
        constant += "/>";
        sb.appendHtmlConstant(constant);
    }
}
