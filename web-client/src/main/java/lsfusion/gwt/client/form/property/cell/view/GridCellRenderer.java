package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;

public interface GridCellRenderer {
    void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);
    void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
