package lsfusion.gwt.client.form.object.table;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.grid.view.GCustom;
import lsfusion.gwt.client.form.object.table.grid.view.GStateTableView;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class TableContainer extends ResizableSimplePanel implements HasMaxPreferredSize {

    private final GFormController form;
    private TableComponent tableComponent;

    public TableContainer(GFormController form) {
        setStyleName("tableContainer");

        this.form = form;
        getFocusElement().setTabIndex(0);

        DataGrid.initSinkEvents(this);
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

    public void updateElementClass(GGridProperty component) {
        GFormLayout.updateComponentClass(component.valueClass, tableComponent.getWidget(), "value");
    }

    public void changeTableComponent(TableComponent tableComponent, boolean boxed) {
        this.tableComponent = tableComponent;
        Widget widget = tableComponent.getWidget();
        if(tableComponent instanceof GStateTableView)
            setPercentMain(widget);
        else
            setWidget(widget);

        addHandler(tableComponent.getScrollHandler(), ScrollEvent.getType(), Event.ONSCROLL);
        addHandler(tableComponent.getMouseWheelScrollHandler(), MouseWheelEvent.getType(), Event.ONMOUSEWHEEL);
        addHandler(tableComponent.getTouchMoveHandler(), TouchMoveEvent.getType(), Event.ONTOUCHMOVE);

        if(boxed)
            addStyleName("tableContainerBoxed");
        else
            removeStyleName("tableContainerBoxed");

        tableComponent.onActivate();
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = form.getTargetAndPreview(getElement(), event);
        if(target == null)
            return;

        super.onBrowserEvent(event);

        lsfusion.gwt.client.base.view.EventHandler eventHandler = new lsfusion.gwt.client.base.view.EventHandler(event);
        DataGrid.dispatchFocusAndCheckSinkEvents(eventHandler, target, getElement(), this::onFocus, this::onBlur);
        if(eventHandler.consumed)
            return;

        tableComponent.onBrowserEvent(target, eventHandler);
    }

    public boolean isFocused;
    protected void onFocus(Element target, lsfusion.gwt.client.base.view.EventHandler eventHandler) {
        if(isFocused)
            return;
        isFocused = true;
        addStyleName("tableContainerFocused");
        focusedChanged(target);
    }

    public void focusedChanged(Element target) {
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

    protected void onBlur(Element target, lsfusion.gwt.client.base.view.EventHandler eventHandler) {
        if(!isFocused)
            return;
        isFocused = false;
        removeStyleName("tableContainerFocused");
        focusedChanged(target);
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
