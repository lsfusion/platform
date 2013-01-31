package platform.gwt.cellview.client;

import com.google.gwt.user.client.ui.CustomScrollPanel;
import com.google.gwt.user.client.ui.HorizontalScrollbar;
import com.google.gwt.user.client.ui.Widget;

import static com.google.gwt.dom.client.Style.Overflow;

public class DataGridScrollPanel extends CustomScrollPanel {
    public DataGridScrollPanel(Widget child) {
        super(child);
    }

    public void removeScrollbars() {
        super.removeHorizontalScrollbar();
        super.removeVerticalScrollbar();
        getScrollableElement().getStyle().setOverflow(Overflow.HIDDEN);
    }

    /**
     * Remove the {@link com.google.gwt.user.client.ui.HorizontalScrollbar}, if one exists.
     */
    public void removeHorizontalScrollbar() {
        super.removeHorizontalScrollbar();
        getScrollableElement().getStyle().setOverflowX(Overflow.HIDDEN);
    }

    /**
     * Remove the {@link com.google.gwt.user.client.ui.VerticalScrollbar}, if one exists.
     */
    public void removeVerticalScrollbar() {
        super.removeVerticalScrollbar();
        getScrollableElement().getStyle().setOverflowY(Overflow.HIDDEN);
    }

    public int getRealClientHeight() {
        int height = getClientHeight() - getHorizontalScrollbarHeight();
        //if pane height is smaller than scrollbar height, than return 0
        return height < 0 ? 0 : height;
    }

    public int getHorizontalScrollbarHeight() {
        HorizontalScrollbar scrollBar = getHorizontalScrollbar();
        return scrollBar == null ? 0 : scrollBar.asWidget().getOffsetHeight();
    }

    public int getClientHeight() {
        return getElement().getClientHeight();
    }

    public int getClientWidth() {
        return getElement().getClientWidth();
    }
}
