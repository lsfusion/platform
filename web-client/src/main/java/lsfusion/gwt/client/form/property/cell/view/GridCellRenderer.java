package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.base.view.grid.DataGrid;

public interface GridCellRenderer {
    void renderDom(DataGrid table, DivElement cellElement, Object value);
    void updateDom(DivElement cellElement, DataGrid table, Object value);

    void renderDom(DivElement cellElement, Object value);
    void updateDom(DivElement cellElement,  Object value);
}
