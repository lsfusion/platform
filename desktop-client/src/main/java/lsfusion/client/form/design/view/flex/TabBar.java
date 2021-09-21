package lsfusion.client.form.design.view.flex;

import lsfusion.client.form.design.view.widget.Widget;

public interface TabBar {
    Widget asWidget();

    void removeTab(int idx);
    void insertTab(Widget tabWidget, int beforeIndex);

    int getSelectedTab();
    boolean selectTab(int index);

    void setTabText(int index, String caption);
}
