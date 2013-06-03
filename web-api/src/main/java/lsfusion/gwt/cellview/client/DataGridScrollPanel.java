package lsfusion.gwt.cellview.client;

import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import static com.google.gwt.dom.client.Style.Overflow;

//public class DataGridScrollPanel extends CustomScrollPanel {
public class DataGridScrollPanel extends ScrollPanel {
    public DataGridScrollPanel(Widget child) {
        super(child);
    }

    public void removeScrollbars() {
        getScrollableElement().getStyle().setOverflow(Overflow.HIDDEN);
    }

    /**
     * Remove the {@link com.google.gwt.user.client.ui.HorizontalScrollbar}, if one exists.
     */
    public void removeHorizontalScrollbar() {
        getScrollableElement().getStyle().setOverflowX(Overflow.HIDDEN);
    }

    /**
     * Remove the {@link com.google.gwt.user.client.ui.VerticalScrollbar}, if one exists.
     */
    public void removeVerticalScrollbar() {
        getScrollableElement().getStyle().setOverflowY(Overflow.HIDDEN);
    }

    public int getHorizontalScrollbarHeight() {
        return getScrollableElement().getOffsetHeight() - getScrollableElement().getClientHeight();
    }

    public int getVerticalScrollbarWidth() {
        return getScrollableElement().getOffsetWidth() - getScrollableElement().getClientWidth();
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
}
