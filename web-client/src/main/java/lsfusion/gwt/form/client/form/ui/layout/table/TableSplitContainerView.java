package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.client.form.ui.layout.SplitContainerView;
import lsfusion.gwt.form.shared.view.GContainer;

public class TableSplitContainerView extends SplitContainerView<CellPanel> {
    public TableSplitContainerView(GContainer container) {
        super(container);
    }

    @Override
    protected TableSplitPanel createSplitPanel(boolean vertical) {
        return new TableSplitPanel(vertical);
    }

    @Override
    protected Widget wrapWithCaption(CellPanel panel) {
        return wrapWithTableCaption(panel);
    }
}
