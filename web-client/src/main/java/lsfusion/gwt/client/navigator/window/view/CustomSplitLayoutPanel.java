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

public class CustomSplitLayoutPanel extends DockLayoutPanel {
    private ScheduledCommand layoutCommand;
    
    public CustomSplitLayoutPanel() {
        super(Unit.PX);
    }

    @Override
    public void insertNorth(Widget widget, double size, Widget before) {
        super.insertNorth(widget, size, before);
    }

    @Override
    protected void insert(Widget widget, Direction direction, double size, Widget before) {
        super.insert(widget, direction, size, before);

        boolean vertical = direction == Direction.NORTH || direction == Direction.SOUTH || (direction == Direction.CENTER && hasDirectionChild(Direction.SOUTH));
        boolean horizontal = direction == Direction.WEST || direction == Direction.EAST || (direction == Direction.CENTER && hasDirectionChild(Direction.EAST));

        if (vertical) {
            addResizeHandler(widget, direction, true);
        }
        if (horizontal) {
            addResizeHandler(widget, direction, false);
        }
        if (vertical || horizontal) {
            DataGrid.initSinkMouseEvents(widget);
        }

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
    
    private boolean hasDirectionChild(Direction direction) {
        for (int i = getChildren().size() - 1; i >= 0; i--) {
            LayoutData childLD = (LayoutData) getChildren().get(i).getLayoutData();
            if (childLD.direction == direction) {
                return true;
            }
        }
        return false;
    }
    
    private void addResizeHandler(Widget widget, Direction direction, boolean vertical) {
        ResizeHelper resizeHelper = createResizeHelper(widget, direction, vertical);

        widget.addHandler(event -> onMouseEvent(resizeHelper, widget, event),
                MouseDownEvent.getType());
        widget.addHandler(event -> onMouseEvent(resizeHelper, widget, event),
                MouseMoveEvent.getType());
    }
    
    private void onMouseEvent(ResizeHelper resizeHelper, Widget widget, DomEvent event) {
        ResizeHandler.checkResizeEvent(resizeHelper, widget.getElement(), null, event.getNativeEvent());
    }
    
    private ResizeHelper createResizeHelper(Widget widget, Direction direction, boolean vertical) {
        return new ResizeHelper() {
            @Override
            public int getChildCount() {
                return 1;
            }

            @Override
            public int getChildAbsolutePosition(int index, boolean left) {
                return ResizeHandler.getAbsolutePosition(widget.getElement(), vertical, left);
            }

            @Override
            public void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement) {}

            @Override
            public double resizeChild(int index, int delta) {
                if (direction == Direction.CENTER) {
                    // emulate resizing of center child by resizing south or east neighbour 
                    for (int i = getChildren().size() - 1; i >= 0; i--) {
                        LayoutData childLD = (LayoutData) getChildren().get(i).getLayoutData();
                        if ((isVertical() && childLD.direction == Direction.SOUTH) || (!isVertical() && childLD.direction == Direction.EAST)) {
                            double newSize = childLD.size - delta;
                            childLD.size = Math.max(newSize, 0);
                            break;
                        }
                    }
                } else {
                    LayoutData ld = (LayoutData) widget.getLayoutData();
                    double newSize = ld.size + delta;
                    ld.size = Math.max(newSize, 0);
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
            public boolean isChildResizable(int index) {
                return true;
            }

            @Override
            public boolean isVertical() {
                return vertical;
            }
        };
    }
}