package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.layout.TabbedContainerView;
import lsfusion.gwt.form.shared.view.GContainer;

public class TableTabbedContainerView extends TabbedContainerView {
    public TableTabbedContainerView(final GFormController formController, final GContainer container) {
        super(formController, container, new TabbedPane());
    }

    protected static class TabbedPane extends TableTabbedPanel implements TabbedDelegate {
        @Override
        public void insertTab(Widget child, String tabTitle, int index) {
            child.setSize("100%", "100%");
            insert(child, tabTitle, index);
        }
    }
}
