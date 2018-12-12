package lsfusion.gwt.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

public interface TabDeck extends RequiresResize {
    void showWidget(int tabIndex);

    void insert(Widget widget, int beforeIndex);

    boolean remove(int index);

    int getWidgetCount();

    int getWidgetIndex(Widget widget);

    Widget getWidget(int index);
}
