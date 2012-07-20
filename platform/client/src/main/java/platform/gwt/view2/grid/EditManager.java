package platform.gwt.view2.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;

public interface EditManager {
    public boolean isCurrentlyEditing();

    void executePropertyEditAction(GridEditableCell gridEditableCell, Cell.Context context, Element parent);

    void commitEditing(Object value);

    void cancelEditing();
}
