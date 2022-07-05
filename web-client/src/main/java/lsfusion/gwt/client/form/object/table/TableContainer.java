package lsfusion.gwt.client.form.object.table;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;

import static com.google.gwt.dom.client.Style.Overflow.AUTO;

public class TableContainer extends ResizableSimplePanel implements HasMaxPreferredSize {
    private static final String STYLE_NAME = "tableContainer";
    private static final String BOXED_STYLE_NAME = "tableContainerBoxed";
    private static final String FOCUSED_STYLE_NAME = "tableContainerFocused";
    
    public final boolean autoSize;
    private final GFormController form;
    private TableComponent tableComponent;

    public TableContainer(boolean autoSize, GFormController form) {
        this.autoSize = autoSize;

        
        setStyleName(STYLE_NAME);

        this.form = form;

        getElement().getStyle().setOutlineStyle(Style.OutlineStyle.NONE);
        getElement().getStyle().setOverflow(AUTO);

        // in theory margin should be set for inner (child element)
        // but 1) margin-right doesn't work for table if it's width 100%
        // 2) it's hard to tell what to do with scroller, since we want right border when there is scroll, and don't wan't it when there is no such scroll
        // however we can remove margin when there is a vertical scroller (so there's no difference whether to set border for child or parent)
        DataGrid.removeOuterGridBorders(this);
        
        DataGrid.initSinkFocusEvents(this);
    }

    protected com.google.gwt.dom.client.Element getScrollableElement() {
        return getElement();
    }

    public int getClientHeight() {
        return getScrollableElement().getClientHeight();
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
        Event.sinkEvents(getScrollableElement(), Event.ONSCROLL);
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
            addStyleName(BOXED_STYLE_NAME);
        else
            removeStyleName(BOXED_STYLE_NAME);
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
        if(tableComponent instanceof DataGrid) { // we have to propagate focus to grid, since GWT proceeds the FOCUS event for the first widget that have eventListener (i.e initSinkEvents is called)
            DataGrid<?> grid = (DataGrid<?>) tableComponent;
            grid.onFocus();
            grid.onGridBrowserEvent(target, event);
        }

        if(isFocused)
            return;
        isFocused = true;
        addStyleName(FOCUSED_STYLE_NAME);
    }

    protected void onBlur(Element target, Event event) {
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
        removeStyleName(FOCUSED_STYLE_NAME);
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
