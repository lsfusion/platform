package lsfusion.client.form.design.view.flex;

public interface TabBar {
    void removeTab(int idx);
    void insertTab(String tabText, int beforeIndex);

    int getSelectedTab();
    boolean selectTab(int index);

    void setTabText(int index, String caption);
}
