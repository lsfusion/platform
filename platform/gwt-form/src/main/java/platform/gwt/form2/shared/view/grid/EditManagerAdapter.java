package platform.gwt.form2.shared.view.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

public class EditManagerAdapter implements EditManager {
    @Override
    public boolean canStartNewEdit() {
        return true;
    }

    @Override
    public GPropertyDraw getProperty(int row, int column) {
        return null;
    }

    @Override
    public GGroupObjectValue getColumnKey(int row, int column) {
        return null;
    }

    @Override
    public void executePropertyEditAction(GridEditableCell editCell, NativeEvent editEvent, Cell.Context context, Element parent) {
    }

    @Override
    public void commitEditing(Object value) {
    }

    @Override
    public void cancelEditing() {
    }
}
