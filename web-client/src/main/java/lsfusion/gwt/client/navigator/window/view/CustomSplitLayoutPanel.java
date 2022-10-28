package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.resize.ResizeHandler;
import lsfusion.gwt.client.base.resize.ResizeHelper;
import lsfusion.gwt.client.base.view.grid.DataGrid;

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
        
        com.google.gwt.user.client.Element widgetElement = widget.getElement();
        boolean vertical = direction == Direction.NORTH || direction == Direction.SOUTH;

        ResizeHelper resizeHelper = new ResizeHelper() {
            @Override
            public int getChildCount() {
                return 1;
            }

            @Override
            public int getChildAbsolutePosition(int index, boolean left) {
                return ResizeHandler.getAbsolutePosition(widgetElement, vertical, left);
            }

            @Override
            public void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement) {}

            @Override
            public double resizeChild(int index, int delta) {
                double newSize = ((LayoutData) widget.getLayoutData()).size + delta;
                ((LayoutData) widget.getLayoutData()).size = Math.max(newSize, 0);
                
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
        
        if (direction != Direction.CENTER) {
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
            widgetElement.getStyle().setProperty(borderPropertyName, "1px solid var(--panel-border-color)");
            
            widget.addHandler(event -> ResizeHandler.checkResizeEvent(resizeHelper, widgetElement, null, event.getNativeEvent()),
                    MouseDownEvent.getType());
            widget.addHandler(event -> ResizeHandler.checkResizeEvent(resizeHelper, widgetElement, null, event.getNativeEvent()),
                    MouseMoveEvent.getType());
            
            DataGrid.initSinkMouseEvents(widget);
        }
    }
}