package lsfusion.gwt.client.form.object.table;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;

import static com.google.gwt.dom.client.Style.Overflow.AUTO;

public class TableContainer extends ResizableSimplePanel implements HasMaxPreferredSize {

    public final boolean autoSize;
    private final GFormController form;
    private TableComponent tableComponent;
    
    private Event pendingFocusEvent;

    public TableContainer(boolean autoSize, GFormController form) {
        this.autoSize = autoSize;
        
        setStyleName("tableContainer");

        this.form = form;

        getElement().getStyle().setOutlineStyle(Style.OutlineStyle.NONE);
        getElement().getStyle().setOverflow(AUTO);

        DataGrid.initSinkFocusEvents(this);
    }

    protected com.google.gwt.dom.client.Element getScrollableElement() {
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

    public HandlerRegistration addScrollHandler(ScrollHandler handler) {
        /*
         * Sink the event on the scrollable element, which may not be the root
         * element.
         */
        sinkEvents(Event.ONSCROLL);
        return addHandler(handler, ScrollEvent.getType());
    }

    public TableComponent getTableComponent() {
        return tableComponent;
    }

    public void changeTableComponent(TableComponent tableComponent, boolean boxed) {
        this.tableComponent = tableComponent;
        setSizedWidget(tableComponent.getWidget(), autoSize);
        ScrollHandler scrollHandler = tableComponent.getScrollHandler();
        if (scrollHandler != null) {
            addScrollHandler(scrollHandler);
        }

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
            grid.onFocus();
            grid.onGridBrowserEvent(target, focusEvent);
            return true;
        }
        return false;
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getTargetAndCheck(getElement(), event);
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
    }

    protected void onBlur(Element target, Event event) {
        pendingFocusEvent = null;
        
        // should be before isFakeBlur check to propagate the event to the cell editor
        if(tableComponent instanceof DataGrid) {
            DataGrid<?> grid = (DataGrid<?>) tableComponent;
            grid.onBlur(event);
            grid.onGridBrowserEvent(target, event);
        }

        if(!isFocused || DataGrid.isFakeBlur(event, getElement())) {
            return;
        }
        isFocused = false;
        removeStyleName("tableContainerFocused");
    }

    public void setPreferredSize(boolean set, Result<Integer> grids) {
        if(!autoSize)
            changePercentFillWidget(set);

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
