package platform.gwt.form.shared.view.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

public interface EditManager {
    public boolean canStartNewEdit();

    GPropertyDraw getProperty(Cell.Context context);
    GGroupObjectValue getColumnKey(Cell.Context context);

    void executePropertyEditAction(GridEditableCell editCell, NativeEvent editEvent, Cell.Context context, Element parent);

    void commitEditing(Object value);

    void cancelEditing();
}
