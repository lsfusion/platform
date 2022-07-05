package lsfusion.gwt.client.form.object.table;

import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public interface TableComponent {
    Widget getWidget();

    default ScrollHandler getScrollHandler() {
        return null;
    }
    
    default void onTableContainerLoad() {}
    default void onTableContainerUnload() {}
    default void onResize() {}
    default void onBrowserEvent(Event event) {}
}
