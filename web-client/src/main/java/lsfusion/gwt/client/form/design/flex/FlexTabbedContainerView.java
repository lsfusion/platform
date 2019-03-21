package lsfusion.gwt.client.form.design.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.ui.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.TabbedContainerView;
import lsfusion.gwt.shared.view.GComponent;
import lsfusion.gwt.shared.view.GContainer;

public class FlexTabbedContainerView extends TabbedContainerView {

    public FlexTabbedContainerView(final GFormController formController, final GContainer container) {
        super(formController, container, new Panel());
    }

    protected static class Panel extends FlexTabbedPanel implements TabbedDelegate {
        @Override
        public void insertTab(GComponent child, Widget childView, String tabTitle, int index) {
            FlexPanel proxyPanel = new FlexPanel(true);
            proxyPanel.add(childView, child.getAlignment(), child.getFlex());

            child.installPaddings(proxyPanel);

            insert(proxyPanel, tabTitle, index);
        }
    }
}
