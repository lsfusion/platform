package lsfusion.gwt.client.form.object.table;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.view.GCustom;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class TableContainer extends ResizableSimplePanel implements HasMaxPreferredSize {

    private final GFormController form;
    private TableComponent tableComponent;
    
    private Event pendingFocusEvent;

    public TableContainer(GFormController form) {
        setStyleName("tableContainer");

        this.form = form;
        getFocusElement().setTabIndex(0);

        DataGrid.initSinkFocusEvents(this);
    }

    public com.google.gwt.dom.client.Element getFocusElement() {
        return getElement();
    }

    public com.google.gwt.dom.client.Element getScrollableElement() {
        return getElement();
    }

    public int getClientHeight() {
        return getScrollableElement().getClientHeight();
    }

    public int getWidth() {
        return GwtClientUtils.getWidth(getScrollableElement());
    }

    public int getClientWidth() {
        return getScrollableElement().getClientWidth();
    }

    public int getOffsetHeight() {
        return getScrollableElement().getOffsetHeight();
    }

    public int getOffsetWidth() {
        return getScrollableElement().getOffsetWidth();
    }

    public int getHorizontalScrollPosition() {
        return getScrollableElement().getScrollLeft();
    }

    public void setHorizontalScrollPosition(int position) {
        getScrollableElement().setScrollLeft(position);
    }

    public void setVerticalScrollPosition(int position) {
        getScrollableElement().setScrollTop(position);
    }

    public int getVerticalScrollPosition() {
        return getScrollableElement().getScrollTop();
    }
    
    public int getScrollHeight() {
        return getScrollableElement().getScrollHeight();
    }

    private <H extends EventHandler> void addHandler(H handler, DomEvent.Type<H> type, int event) {
        /*
         * Sink the event on the scrollable element, which may not be the root
         * element.
         */
        if (handler != null) {
            sinkEvents(event);
            addHandler(handler, type);
        }
    }

    public TableComponent getTableComponent() {
        return tableComponent;
    }

    public void changeTableComponent(TableComponent tableComponent, boolean boxed) {
        this.tableComponent = tableComponent;
        setPercentMain(tableComponent.getWidget());

        addHandler(tableComponent.getScrollHandler(), ScrollEvent.getType(), Event.ONSCROLL);
        addHandler(tableComponent.getMouseWheelScrollHandler(), MouseWheelEvent.getType(), Event.ONMOUSEWHEEL);
        addHandler(tableComponent.getTouchMoveHandler(), TouchMoveEvent.getType(), Event.ONTOUCHMOVE);

        if(boxed)
            addStyleName("tableContainerBoxed");
        else
            removeStyleName("tableContainerBoxed");
        
        if (pendingFocusEvent != null) {
            propagateFocusToGrid(DataGrid.getTargetAndCheck(getElement(), pendingFocusEvent), pendingFocusEvent);
            pendingFocusEvent = null;
        }
    }

    private boolean propagateFocusToGrid(Element target, Event focusEvent) {
        if (tableComponent instanceof DataGrid) { // we have to propagate focus to grid, since GWT proceeds the FOCUS event for the first widget that have eventListener (i.e initSinkEvents is called)
            DataGrid<?> grid = (DataGrid<?>) tableComponent;
            grid.onFocus(target, focusEvent);
            grid.onGridBrowserEvent(target, focusEvent);
            return true;
        }
        return false;
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getBrowserTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!form.previewEvent(target, event))
            return;

        tableComponent.onBrowserEvent(event);

        super.onBrowserEvent(event);

        if(!DataGrid.checkSinkFocusEvents(event))
            return;

        String eventType = event.getType();
        if (BrowserEvents.FOCUS.equals(eventType))
            onFocus(target, event);
        else if (BrowserEvents.BLUR.equals(eventType))
            onBlur(target, event);

        form.propagateFocusEvent(event);
    }

    private boolean isFocused;
    protected void onFocus(Element target, Event event) {
        if (!propagateFocusToGrid(target, event)) {
            // if focus event comes via focusLastBlurredElement() - by switching between table views in toolbar,
            // it happens before changing of table component (here - from any other to grid). so we postpone propagation until change is finished   
            pendingFocusEvent = event;
        }

        if(isFocused)
            return;
        isFocused = true;
        addStyleName("tableContainerFocused");
        focusedChanged(target, event);
    }

    public void focusedChanged(Element target, Event focusEvent) {
        Element drawElement = null;
        if(tableComponent instanceof GCustom)
            drawElement = ((GCustom) tableComponent).getDrawElement();
        form.checkFocusElement(isFocused, drawElement);
        if(isFocused && drawElement != null) {
            Object focusElement = CellRenderer.getFocusElement(drawElement);
            if (focusElement != null && focusElement != CellRenderer.NULL && focusElement != target) { // last check - optimization
                FocusUtils.Reason reason = FocusUtils.getFocusReason(target);
                if (reason == null)
                    reason = FocusUtils.Reason.OTHER;
                FocusUtils.focus((Element) focusElement, reason);
            }
        }
    }

    protected void onBlur(Element target, Event event) {
        pendingFocusEvent = null;
        
        // should be before isFakeBlur check to propagate the event to the cell editor
        if(tableComponent instanceof DataGrid) {
            DataGrid<?> grid = (DataGrid<?>) tableComponent;
            grid.onBlur(target, event);
            grid.onGridBrowserEvent(target, event);
        }

        if(!isFocused || FocusUtils.isFakeBlur(event, getElement())) {
            return;
        }
        isFocused = false;
        removeStyleName("tableContainerFocused");
        focusedChanged(target, event);
    }

    public void setPreferredSize(boolean set, Result<Integer> grids) {
        if(tableComponent instanceof HasMaxPreferredSize) // needed to setPreferredSize for grid
            ((HasMaxPreferredSize) tableComponent).setPreferredSize(set, grids);
    }

    @Override
    public void onResize() {
        tableComponent.onResize();
        super.onResize();
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        tableComponent.onTableContainerLoad();
    }

    @Override
    protected void onUnload() {
        tableComponent.onTableContainerUnload();
        super.onUnload();
    }
}
