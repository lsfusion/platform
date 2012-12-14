package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import platform.gwt.cellview.client.cell.Cell;

public abstract class AbstractGridCellEditor implements GridCellEditor {
    @Override
    public abstract void renderDom(Cell.Context context, DivElement cellParent, Object value);
}
