package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.ui.cellview.DataGrid;
import lsfusion.gwt.client.base.ui.cellview.cell.Cell;

public interface GridCellEditor {

    void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value);

    void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event);

    void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue);

    boolean replaceCellRenderer();
}
