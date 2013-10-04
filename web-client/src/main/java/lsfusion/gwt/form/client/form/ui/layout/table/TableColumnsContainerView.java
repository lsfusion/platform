package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.DivWidget;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.form.client.form.ui.layout.ColumnsContainerView;
import lsfusion.gwt.form.shared.view.GContainer;

public class TableColumnsContainerView extends ColumnsContainerView<ResizableHorizontalPanel> {
    public TableColumnsContainerView(GContainer container) {
        super(container);

        DivWidget fill = new DivWidget();
        panel.add(fill);
        panel.setCellWidth(fill, "100%");
    }

    @Override
    protected ResizableHorizontalPanel createHorizontalPanel() {
        return new ResizableHorizontalPanel();
    }

    @Override
    protected Widget wrapWithCaption(ResizableHorizontalPanel panel) {
        return  wrapWithTableCaption(panel);
    }
}
