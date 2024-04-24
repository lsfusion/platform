package lsfusion.gwt.client.form.object.table;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.EventHandler;

public interface TableComponent {
    Widget getWidget();

    default <H extends ScrollHandler> H getScrollHandler() {
        return null;
    }

    default <H extends MouseWheelHandler> H getMouseWheelScrollHandler() {
        return null;
    }

    default <H extends TouchMoveHandler> H getTouchMoveHandler() {
        return null;
    }
    
    default void onTableContainerLoad() {}
    default void onTableContainerUnload() {}
    default void onResize() {}
    default void onBrowserEvent(Element target, EventHandler event) {}
    default void onActivate() {}
}
