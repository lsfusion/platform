package lsfusion.gwt.form.client.form.ui.dialog;

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
    
    private boolean isDragging = false;

    public WindowDragDropController(AbsolutePanel boundaryPanel) {
        this.boundaryPanel = boundaryPanel;

        pickupDragController = new PickupDragController(boundaryPanel, true) {
            @Override
            protected BoundaryDropController newBoundaryDropController(AbsolutePanel boundaryPanel, boolean allowDroppingOnBoundaryPanel) {
                return new WindowDropController(boundaryPanel, allowDroppingOnBoundaryPanel);
            }

            @Override
            public void dragStart() {
                isDragging = true;
                super.dragStart();
            }

            @Override
            public void dragEnd() {
                super.dragEnd();
                isDragging = false;
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
    
    public boolean isDragging() {
        return isDragging;
    }

    public PickupDragController getPickupDragController() {
        return pickupDragController;
    }

    public ResizeDragController getResizeDragController() {
        return resizeDragController;
    }

    public class WindowDropController extends BoundaryDropController {

        public WindowDropController(AbsolutePanel dropTarget, boolean allowDroppingOnBoundaryPanel) {
            super(dropTarget, allowDroppingOnBoundaryPanel);
        }

        @Override
        public void onDrop(DragContext context) {
            super.onDrop(context);

            //передобавляем в конец, иначе по z-order показываются под остальными элементами,
            //при передобавлении координаты сбрасываются поэтому сначала запоминаем их

            Widget draggable = context.draggable;
            int draggableLeft = draggable.getAbsoluteLeft();
            int draggableTop = draggable.getAbsoluteTop();
            boundaryPanel.add(draggable);
            boundaryPanel.setWidgetPosition(draggable, draggableLeft, draggableTop);
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