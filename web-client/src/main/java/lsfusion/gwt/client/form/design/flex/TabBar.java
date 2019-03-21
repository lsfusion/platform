package lsfusion.gwt.client.form.design.flex;

import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public interface TabBar {
    Widget asWidget();

    void removeTab(int idx);
    void insertTab(Widget tabWidget, int beforeIndex);

    int getSelectedTab();
    boolean selectTab(int index);

    HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> beforeSelectionHandler);
    HandlerRegistration addSelectionHandler(SelectionHandler<Integer> selectionHandler);
}
