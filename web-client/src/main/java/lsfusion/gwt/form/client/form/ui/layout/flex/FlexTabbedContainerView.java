package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.layout.TabbedContainerView;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

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
