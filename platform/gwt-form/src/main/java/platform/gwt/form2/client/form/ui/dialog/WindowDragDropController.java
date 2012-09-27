package platform.gwt.form2.client.form.ui.dialog;

import com.allen_sauer.gwt.dnd.client.AbstractDragController;
import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.BoundaryDropController;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public final class WindowDragDropController {
    public static final WindowDragDropController rootController = new WindowDragDropController(RootPanel.get());

    private final AbsolutePanel boundaryPanel;

    private PickupDragController pickupDragController;

    private ResizeDragController resizeDragController;

    public WindowDragDropController(AbsolutePanel boundaryPanel) {
        this.boundaryPanel = boundaryPanel;

        pickupDragController = new PickupDragController(boundaryPanel, true) {
            @Override
            protected BoundaryDropController newBoundaryDropController(AbsolutePanel boundaryPanel, boolean allowDroppingOnBoundaryPanel) {
                return new MyDropController(boundaryPanel, allowDroppingOnBoundaryPanel);
            }
        };
        pickupDragController.setBehaviorConstrainedToBoundaryPanel(true);
        pickupDragController.setBehaviorMultipleSelection(false);

        resizeDragController = new ResizeDragController();
        resizeDragController.setBehaviorConstrainedToBoundaryPanel(true);
        resizeDragController.setBehaviorMultipleSelection(false);
    }

    public AbsolutePanel getBoundaryPanel() {
        return boundaryPanel;
    }

    public PickupDragController getPickupDragController() {
        return pickupDragController;
    }

    public ResizeDragController getResizeDragController() {
        return resizeDragController;
    }

    public class MyDropController extends BoundaryDropController {

        public MyDropController(AbsolutePanel dropTarget, boolean allowDroppingOnBoundaryPanel) {
            super(dropTarget, allowDroppingOnBoundaryPanel);
        }

        @Override
        public void onDrop(DragContext context) {
            super.onDrop(context);
//
            //передобавляем в конец, иначе по z-order показываются под остальными элементами
            boundaryPanel.add(context.draggable);
            boundaryPanel.setWidgetPosition(context.draggable, context.desiredDraggableX, context.desiredDraggableY);
        }
    }

    public final class ResizeDragController extends AbstractDragController {
        public ResizeDragController() {
            super(boundaryPanel);
        }

        @Override
        public void dragMove() {
            ((ResizableWindow.EdgeWidget) context.draggable).dragMove(context);
        }

        @Override
        public final void makeDraggable(Widget draggable) {
            throw new IllegalStateException("Shouldn't be used directly");
        }

        public void addDraggableEdge(ResizableWindow.EdgeWidget draggable) {
            super.makeDraggable(draggable);
        }
    }
}