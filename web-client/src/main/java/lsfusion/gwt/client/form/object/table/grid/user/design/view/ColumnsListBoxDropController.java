package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.drop.AbstractDropController;
import com.allen_sauer.gwt.dnd.client.util.*;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

import static lsfusion.gwt.client.view.MainFrame.v5;

public class ColumnsListBoxDropController extends AbstractDropController {

    private ColumnsListBox mouseListBox;

    private Widget positioner = null;

    private int targetRow;

    private InsertPanel listBoxRowsAsIndexPanel = new InsertPanel() {
        @Override
        public void add(Widget w) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Widget getWidget(int index) {
            return mouseListBox.getWidget(index);
        }

        @Override
        public int getWidgetCount() {
            return mouseListBox.getItemCount();
        }

        @Override
        public int getWidgetIndex(Widget child) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void insert(Widget w, int beforeIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(int index) {
            throw new UnsupportedOperationException();
        }
    };

    public ColumnsListBoxDropController(ColumnsListBox mouseListBox) {
        super(mouseListBox);
        this.mouseListBox = mouseListBox;
    }

    @Override
    public void onDrop(DragContext context) {
        ColumnsListBox from = (ColumnsListBox) context.draggable.getParent().getParent();
        int i = 1;
        for (Widget widget : context.selectedWidgets) {
            if (widget != null && widget.getParent() != null && widget.getParent().getParent() == from) {
                mouseListBox.add(targetRow + i, ((PropertyLabel) widget).getPropertyItem());
                i++;
            }
        }
        super.onDrop(context);
    }

    @Override
    public void onEnter(DragContext context) {
        super.onEnter(context);
        positioner = newPositioner();
    }

    @Override
    public void onLeave(DragContext context) {
        positioner.removeFromParent();
        positioner = null;
        super.onLeave(context);
    }

    @Override
    public void onMove(DragContext context) {
        super.onMove(context);
        targetRow = DOMUtil.findIntersect(listBoxRowsAsIndexPanel, new CoordinateLocation(
                context.mouseX, context.mouseY), LocationWidgetComparator.BOTTOM_HALF_COMPARATOR) - 1;

        if (mouseListBox.getItemCount() > 0) {
            Widget w = mouseListBox.getWidget(targetRow == -1 ? 0 : targetRow);
            Location widgetLocation = new WidgetLocation(w, context.boundaryPanel);
            Location tableLocation = new WidgetLocation(mouseListBox, context.boundaryPanel);
            context.boundaryPanel.add(positioner, tableLocation.getLeft(), widgetLocation.getTop()
                    + (targetRow == -1 ? 0 : w.getOffsetHeight()));
        }
    }

    private Widget newPositioner() {
        Widget p = new SimplePanel();
        GwtClientUtils.addClassName(p, "list-positioner", "listPositioner", v5);
        p.setPixelSize(mouseListBox.getParent().getOffsetWidth(), 1);
        return p;
    }
}