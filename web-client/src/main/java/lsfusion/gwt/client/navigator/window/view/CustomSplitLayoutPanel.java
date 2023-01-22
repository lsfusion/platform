package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.resize.ResizeHelper;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.google.gwt.user.client.ui.DockLayoutPanel.Direction.*;
import static lsfusion.gwt.client.base.resize.ResizeHandler.checkResizeEvent;

public class CustomSplitLayoutPanel extends DockLayoutPanel {
    private ScheduledCommand layoutCommand;
    
    private ResizeHelper hResizeHelper;
    private ResizeHelper vResizeHelper;
    private List<Widget> hChildren = new ArrayList<>();
    private List<Widget> vChildren = new ArrayList<>();

    public CustomSplitLayoutPanel() {
        super(Unit.PX);

        hResizeHelper = createResizeHelper(false);
        vResizeHelper = createResizeHelper(true);

        addHandler(event -> onMouseEvent(event), MouseDownEvent.getType());
        addHandler(event -> onMouseEvent(event), MouseMoveEvent.getType());
        DataGrid.initSinkMouseEvents(this);
    }

    @Override
    protected void insert(Widget widget, Direction direction, double size, Widget before) {
        super.insert(widget, direction, size, before);

        if (!MainFrame.useBootstrap) {
            String borderPropertyName = "";
            switch (direction) {
                case EAST:
                    borderPropertyName = "borderLeft";
                    break;
                case WEST:
                    borderPropertyName = "borderRight";
                    break;
                case NORTH:
                    borderPropertyName = "borderBottom";
                    break;
                case SOUTH:
                    borderPropertyName = "borderTop";
                    break;
            }
            widget.getElement().getStyle().setProperty(borderPropertyName, "1px solid var(--panel-border-color)");
        }
    }

    protected void refreshResizableChildren() {
        hChildren.clear();
        vChildren.clear();

        for (Widget child : getChildren()) {
            LayoutData ld = (LayoutData) child.getLayoutData();
            if (ld.direction != NORTH && ld.direction != SOUTH) { // with CENTER
                hChildren.add(child);
            }
            if (ld.direction != WEST && ld.direction != EAST) { // with CENTER
                vChildren.add(child);
            }
        }

        // sort children list so that center child is between north-south/west-east
        hChildren.sort(createChildrenComparator(false));
        vChildren.sort(createChildrenComparator(true));
    }
    
    private Comparator<Widget> createChildrenComparator(boolean vertical) {
        return (o1, o2) -> {
            Direction left = vertical ? NORTH : WEST;
            Direction right = vertical ? SOUTH : EAST;
            LayoutData ld1 = (LayoutData) o1.getLayoutData();
            LayoutData ld2 = (LayoutData) o2.getLayoutData();

            if (ld1.direction == ld2.direction) {
                return 0;
            } else if (ld1.direction == left) {
                return -1;
            } else if (ld1.direction == right) {
                return 1;
            }
            // center
            return ld2.direction == left ? 1 : -1;
        };
    }

    private void onMouseEvent(DomEvent event) {
        Element cursorElement = getElement();
        NativeEvent nativeEvent = event.getNativeEvent();

        ResizeHandler.dropCursor(cursorElement, nativeEvent);

        // skip second resize check, because it switches cursor back to default on horizontal resize pointing (without mousedown)
        if (!checkResizeEvent(hResizeHelper, cursorElement, null, nativeEvent)) {
            checkResizeEvent(vResizeHelper, cursorElement, null, nativeEvent);
        }
    }

    private ResizeHelper createResizeHelper(boolean vertical) {
        return new ResizeHelper() {
            private List<Widget> getChildren() {
                return vertical ? vChildren : hChildren;
            }
            
            private Widget getChild(int index) {
                return getChildren().get(index);
            }

            @Override
            public int getChildCount() {
                return getChildren().size();
            }

            @Override
            public int getChildAbsolutePosition(int index, boolean left) {
                return ResizeHandler.getAbsolutePosition(getChild(index).getElement(), vertical, left);
            }

            @Override
            public void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement) {}

            @Override
            public double resizeChild(int index, int delta) {
                LayoutData ld = (LayoutData) getChild(index).getLayoutData();
                if (ld.direction == CENTER) {
                    // emulate resizing of center child by resizing south or east neighbour 
                    for (int i = index + 1; i < getChildCount(); i++) {
                        LayoutData neighbourLD = (LayoutData) getChild(i).getLayoutData();
                        if ((vertical && neighbourLD.direction == SOUTH) || (!vertical && neighbourLD.direction == EAST)) {
                            neighbourLD.size = Math.max(neighbourLD.size - delta, 0);
                            break;
                        }
                    }
                } else {
                    ld.size = Math.max(ld.size + delta, 0);
                }

                // Defer actually updating the layout, so that if we receive many
                // mouse events before layout/paint occurs, we'll only update once.
                if (layoutCommand == null) {
                    layoutCommand = () -> {
                        layoutCommand = null;
                        forceLayout();
                    };
                    Scheduler.get().scheduleDeferred(layoutCommand);
                }
                return 0;
            }

            @Override
            public boolean isResizeOnScroll(int index, NativeEvent event) {
                return false;
            }

            @Override
            public int getScrollSize(int index) {
                return 0;
            }

            @Override
            public boolean isChildResizable(int index) {
                return index != getChildCount() - 1;
            }

            @Override
            public boolean isVertical() {
                return vertical;
            }
        };
    }
}