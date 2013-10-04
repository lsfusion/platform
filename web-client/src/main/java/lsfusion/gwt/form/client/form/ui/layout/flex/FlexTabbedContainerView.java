package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.layout.TabbedContainerView;
import lsfusion.gwt.form.shared.view.GContainer;

public class FlexTabbedContainerView extends TabbedContainerView {

    public FlexTabbedContainerView(final GFormController formController, final GContainer container) {
        super(formController, container, new TabbedPanel());

        FlexTabbedPanel tabsPanel = (FlexTabbedPanel) tabbedDelegate;
        tabsPanel.setMargins(container.marginTop, container.marginBottom, container.marginLeft, container.marginRight);
    }

    protected static class TabbedPanel extends FlexTabbedPanel implements TabbedDelegate {
        @Override
        public void insertTab(Widget child, String tabTitle, int index) {
            FlexPanel proxyPanel = new FlexPanel(true);
            proxyPanel.addFill(child);
            insert(proxyPanel, tabTitle, index);
        }
    }
}
