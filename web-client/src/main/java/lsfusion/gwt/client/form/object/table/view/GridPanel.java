package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;

// needed for auto grid sizing
public class GridPanel extends FlexPanel {
    private final Widget view;
    private final ResizableSimplePanel gridContainerView;

    public GridPanel(Widget view, ResizableSimplePanel gridContainerView) {
        super(true);

        this.view = view;
        addFill(view);
        
        this.gridContainerView = gridContainerView;
    }

    public void autoSize() {
        Widget widget = gridContainerView.getWidget();
        int autoSize;
        if(widget instanceof GGridPropertyTable) {
            GGridPropertyTable gridTable = (GGridPropertyTable)widget;
            if (gridTable instanceof GTreeTable) {
                autoSize = gridTable.getMaxPreferredSize().height;
            } else {
                autoSize = gridTable.getAutoSize();
                if (autoSize <= 0) // еще не было layout'а, ставим эвристичный размер
                    autoSize = gridTable.getMaxPreferredSize().height;
                else {
                    autoSize += view.getOffsetHeight() - gridTable.getViewportHeight(); // margin'ы и border'ы учитываем
                }
            }
        } else
            autoSize = GTreeTable.DEFAULT_MAX_PREFERRED_HEIGHT;
        setChildFlexBasis(view, autoSize);
    }
}
