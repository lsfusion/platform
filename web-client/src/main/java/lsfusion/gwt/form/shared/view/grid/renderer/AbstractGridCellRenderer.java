package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;

public abstract class AbstractGridCellRenderer implements GridCellRenderer {
    protected final String EMPTY_VALUE = "Не определено";
    protected final String REQUIRED_VALUE = "Необходимо заполнить";

    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);

    @Override
    public abstract void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
