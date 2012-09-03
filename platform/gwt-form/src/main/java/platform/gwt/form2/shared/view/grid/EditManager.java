package platform.gwt.form2.shared.view.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

public interface EditManager {
    public boolean canStartNewEdit();

    void executePropertyEditAction(GridEditableCell editCell, NativeEvent editEvent, Cell.Context context, Element parent);

    void commitEditing(Object value);

    void cancelEditing();
}
