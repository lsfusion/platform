package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;

public abstract class AbstractGridCellEditor implements GridCellEditor {
    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value);

    @Override
    public boolean replaceCellRenderer() {
        return true;
    }
}
