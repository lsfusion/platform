package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.allen_sauer.gwt.dnd.client.drop.BoundaryDropController;
import com.allen_sauer.gwt.dnd.client.util.DOMUtil;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

public class ColumnsListBoxDragController extends PickupDragController {

    public ColumnsListBox draggableList;

    public ColumnsListBoxDragController(ColumnsDualListBox dualListBox) {
        super(dualListBox, false);
        setBehaviorDragProxy(true);
        setBehaviorMultipleSelection(true);
        setBehaviorDragStartSensitivity(5);
        setBehaviorConstrainedToBoundaryPanel(true);
    }

    @Override
    public void dragEnd() {
        super.dragEnd();

        if (context.vetoException == null) {
            ColumnsListBox currentMouseListBox = (ColumnsListBox) context.draggable.getParent().getParent();
            while (!context.selectedWidgets.isEmpty()) {
                Widget widget = context.selectedWidgets.get(0);

                toggleSelection(widget);
                currentMouseListBox.remove(widget);
            }
        }
        draggableList = null;
    }

    @Override
    public void previewDragStart() throws VetoDragException {
        super.previewDragStart();
        if (context.selectedWidgets.isEmpty()) {
            throw new VetoDragException();
        }
    }

    @Override
    public void setBehaviorDragProxy(boolean dragProxyEnabled) {
        if (!dragProxyEnabled) {
            throw new IllegalArgumentException();
        }
        super.setBehaviorDragProxy(dragProxyEnabled);
    }

    @Override
    protected BoundaryDropController newBoundaryDropController(AbsolutePanel boundaryPanel,
                                                               boolean allowDroppingOnBoundaryPanel) {
        if (allowDroppingOnBoundaryPanel) {
            throw new IllegalArgumentException();
        }
        return super.newBoundaryDropController(boundaryPanel, allowDroppingOnBoundaryPanel);
    }

    @Override
    public void toggleSelection(Widget draggable) {
        super.toggleSelection(draggable);
        ColumnsListBox currentMouseListBox = (ColumnsListBox) draggable.getParent().getParent();
        ArrayList<Widget> otherWidgets = new ArrayList<Widget>();
        for (Widget widget : context.selectedWidgets) {
            if (widget.getParent().getParent() != currentMouseListBox) {
                otherWidgets.add(widget);
            }
        }
        for (Widget widget : otherWidgets) {
            super.toggleSelection(widget);
        }
    }

    @Override
    protected Widget newDragProxy(DragContext context) {
        draggableList = (ColumnsListBox) context.draggable.getParent().getParent();
        ColumnsListBox proxyMouseListBox = new ColumnsListBox(null) {
            @Override
            public void doubleclicked() {
                // nothing to do
            }
        };
        proxyMouseListBox.setWidth(DOMUtil.getClientWidth(draggableList.getElement()) + "px");
        for (Widget widget : context.selectedWidgets) {
            proxyMouseListBox.add(((PropertyLabel) widget).getPropertyItem());
        }
        return proxyMouseListBox;
    }

    public ArrayList<Widget> getSelectedWidgets(ColumnsListBox mouseListBox) {
        ArrayList<Widget> widgetList = new ArrayList<Widget>();
        for (Widget widget : context.selectedWidgets) {
            if (widget.getParent().getParent() == mouseListBox) {
                widgetList.add(widget);
            }
        }
        return widgetList;
    }
}