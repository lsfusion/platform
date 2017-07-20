package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;

public interface GridCellRenderer {
    void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);
    void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
