package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.drop.AbstractDropController;
import com.google.gwt.user.client.ui.Widget;

public class ColumnsListContainerDropController extends AbstractDropController {
    private ColumnsListContainer listContainer;

    public ColumnsListContainerDropController(ColumnsListContainer dropTarget) {
        super(dropTarget);
        listContainer = dropTarget;
    }

    @Override
    public void onDrop(DragContext context) {
        for (Widget w : context.selectedWidgets) {
            listContainer.getListBox().add(((PropertyLabel) w).getProperty());
        }
    }
}
