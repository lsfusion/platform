package lsfusion.gwt.client.form.property.cell;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.base.ui.cellview.DataGrid;
import lsfusion.gwt.client.base.ui.cellview.cell.Cell;

public interface GridCellRenderer {
    void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value);
    void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value);
}
