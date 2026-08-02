package lsfusion.gwt.client.form.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;

// the standard view: one tab per open form, the platform's own strip. It also carries the platform's toolbar (edit
// mode, full screen, the mobile menu), which is the tab bar's extra widget
public class TabbedFormsView implements FormsView {

    // the marker type FlexPanel's layout rules test for, to tell the forms window's strip from a form's own tabs
    public static class Panel extends FlexTabbedPanel {
        public Panel(Widget extraTabWidget, boolean end) {
            super(extraTabWidget, end);
        }
    }

    private final Panel tabsPanel;

    public TabbedFormsView(Widget toolbarView, boolean tabEnd, FormsView.SelectionHandler selection) {
        tabsPanel = new Panel(toolbarView, tabEnd);

        tabsPanel.setBeforeSelectionHandler(index -> selection.unselected(tabsPanel.getSelectedTab()));
        tabsPanel.setSelectionHandler(selection::selected);

        GwtClientUtils.addClassName(tabsPanel, "forms-container");
    }

    @Override
    public Widget getView() {
        return tabsPanel;
    }

    @Override
    public void formAdded(FormDockable dockable, Integer index) {
        FlexPanel contentWidget = dockable.getContentWidget();

        FlexPanel header = new FlexPanel();
        header.addFillShrink(dockable.getTabWidget());
        header.add(dockable.getCloseButton(), GFlexAlignment.CENTER);

        tabsPanel.addTab(contentWidget, index, header);

        FlexPanel.makeShadowOnScroll(tabsPanel, tabsPanel.getTabBar(), contentWidget, tabsPanel.tabEnd);
    }

    @Override
    public void formRemoved(FormDockable dockable, int index) {
        tabsPanel.removeTab(index);
    }

    @Override
    public void setCurrent(int index) {
        tabsPanel.selectTab(index);
    }

    @Override
    public int getCurrent() {
        return tabsPanel.getSelectedTab();
    }

    @Override
    public void formsChanged() {
        // the tab widget is written as the caption / image / blocked state change, so there is nothing to refresh here
    }
}
