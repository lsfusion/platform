package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class TableScrollPanel extends SimplePanel {

    public TableScrollPanel(Widget child, Style.Overflow overflow) {
        super();

        getElement().getStyle().setOutlineStyle(Style.OutlineStyle.NONE);

//        SimplePanel inner = new SimplePanel();
//        inner.setWidget(child);

        setWidget(child);

//        scrollableElement = child.getElement();

        getElement().getStyle().setOverflow(overflow);

        // in theory margin should be set for inner (child element)
        // but 1) margin-right doesn't work for table if it's width 100%
        // 2) it's hard to tell what to do with scroller, since we want right border when there is scroll, and don't wan't it when there is no such scroll
        // however we can remove margin when there is a vertical scroller (so there's no difference whether to set border for child or parent)
        DataGrid.removeOuterGridBorders(this);
    }

//    private final com.google.gwt.dom.client.Element scrollableElement;

    protected com.google.gwt.dom.client.Element getScrollableElement() {
        return getElement();
    }

    public void setWidget(Widget w) {
//        SimplePanel inner = new SimplePanel();
//        inner.setWidget(w);
//
//        super.setWidget(inner);
        super.setWidget(w);
//        child.setWidth("100%"); // technically we don't need it since upper container has display : block, which is equivalent to width 100%, height:auto
    }
//
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
}
