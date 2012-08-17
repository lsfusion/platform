package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class CellAdapterGridRenderer<C> implements GridCellRenderer {

    private Cell<C> cell;

    public CellAdapterGridRenderer(Cell<C> cell) {
        this.cell = cell;
    }

    @Override
    public void render(Cell.Context context, Object value, SafeHtmlBuilder sb) {
        cell.render(context, (C)value, sb);
    }
}
