package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import platform.gwt.cellview.client.cell.Cell;

public interface GridCellRenderer {
    void renderDom(Cell.Context context, DivElement cellElement, Object value);
    void updateDom(DivElement cellElement, Cell.Context context, Object value);
}
