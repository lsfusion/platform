package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {

    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, Cell.Context context, Object value);
}
