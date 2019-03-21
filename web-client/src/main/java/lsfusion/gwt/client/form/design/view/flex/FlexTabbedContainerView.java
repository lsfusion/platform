package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

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
