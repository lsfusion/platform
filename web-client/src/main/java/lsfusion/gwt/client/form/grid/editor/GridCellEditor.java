package lsfusion.gwt.form.client.grid.editor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.grid.EditEvent;

public interface GridCellEditor {

    void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value);

    void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event);

    void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue);

    boolean replaceCellRenderer();
}
