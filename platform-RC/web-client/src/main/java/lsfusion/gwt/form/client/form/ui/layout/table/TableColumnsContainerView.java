package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ui.DivWidget;
import lsfusion.gwt.base.client.ui.ResizableHorizontalPanel;
import lsfusion.gwt.form.client.form.ui.layout.ColumnsContainerView;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.Map;

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

    @Override
    public Dimension getPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        Dimension result = super.getPreferredSize(containerViews);
        result.width += 1; //DivWidget fill; рендериттся шириной в 1 пиксел
        return result;
    }
}
