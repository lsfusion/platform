package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import platform.gwt.cellview.client.cell.Cell;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {

    @Override
    public abstract void renderDom(Cell.Context context, DivElement cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, Cell.Context context, Object value);
}
