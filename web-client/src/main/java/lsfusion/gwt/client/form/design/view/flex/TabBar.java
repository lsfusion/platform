package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;

public interface TabBar {
    Widget asWidget();

    void removeTab(int idx);
    void insertTab(Widget tabWidget, int beforeIndex);

    int getSelectedTab();
    boolean selectTab(int index);

    void setTabText(int index, String caption);
}
