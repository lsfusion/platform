package platform.gwt.form2.shared.view.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

public interface EditManager {
    public boolean canStartNewEdit();

    GPropertyDraw getProperty(int row, int column);
    GGroupObjectValue getColumnKey(int row, int column);

    void executePropertyEditAction(GridEditableCell editCell, NativeEvent editEvent, Cell.Context context, Element parent);

    void commitEditing(Object value);

    void cancelEditing();
}
