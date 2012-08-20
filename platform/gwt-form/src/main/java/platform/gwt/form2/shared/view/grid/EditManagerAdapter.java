package platform.gwt.form2.shared.view.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;

public class EditManagerAdapter implements EditManager {
    @Override
    public boolean isCurrentlyEditing() {
        return false;
    }

    @Override
    public void executePropertyEditAction(GridEditableCell editCell, Cell.Context context, Element parent) {
    }

    @Override
    public void commitEditing(Object value) {
    }

    @Override
    public void cancelEditing() {
    }
}
