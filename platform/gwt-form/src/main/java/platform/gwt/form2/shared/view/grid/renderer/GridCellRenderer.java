package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public interface GridCellRenderer {

    void render(Cell.Context context, Object value, SafeHtmlBuilder sb);
}
