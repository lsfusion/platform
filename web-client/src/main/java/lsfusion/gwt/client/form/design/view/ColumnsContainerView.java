package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ColumnsContainerView extends GAbstractContainerView {
    protected final static String COLUMN_PROXY_KEY = "columnsProxy";

    protected final int columnsCount;

    protected final FlexPanel panel;

    protected final FlexPanel[] columns;

    protected final List<GComponent>[] columnsChildren;

    protected final Widget view;

    public ColumnsContainerView(GContainer container) {
        super(container);

        assert container.isColumns();

        panel = createHorizontalPanel();

        columnsCount = container.columns ;

        columns = new FlexPanel[columnsCount];
        columnsChildren = new List[columnsCount];
        
        double columnFlex = getColumnFlex(container);
        for (int i = 0; i < columnsCount; ++i) {
            FlexPanel column = new FlexPanel(true);
            panel.add(column);
            column.getElement().getStyle().setProperty("flex", columnFlex + " 0 auto");

            columns[i] = column;
            columnsChildren[i] = new ArrayList<>();
        }
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = initBorder(panel);
    }
    
    private double getColumnFlex(GContainer container) {
        GContainer container2 = container.container;
        if (container2 == null || !container2.isHorizontal()) {
            return container.getAlignment() == GFlexAlignment.STRETCH ? 1 : 0;
        }
        return container.getFlex();
    }

    protected FlexPanel createHorizontalPanel() {
        return new FlexPanel();
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        rebuildColumnCollections();
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        rebuildColumnCollections();
        
        Object columnsProxy = view.getElement().getPropertyObject(COLUMN_PROXY_KEY);
        if (!(columnsProxy instanceof ProxyPanel)) {
            throw new IllegalStateException("Trying to delete something, that wasn't added");
        }
    }

    private void rebuildColumnCollections() {
        clearColumnsCollections();

        int cnt = 0;
        for (GComponent child : container.children) {
            int index = children.indexOf(child);
            if (index >= 0) {
                int colIndex = cnt % columnsCount;
                int rowIndex = cnt / columnsCount;
                columns[colIndex].add(new ProxyPanel(child, childrenViews.get(index)), child.getAlignment());
                columnsChildren[colIndex].add(rowIndex, child);
                ++cnt;
            }
        }
    }

    private void clearColumnsCollections() {
        for (FlexPanel panel : columns) {
            panel.clear();
        }

        for (List<GComponent> components : columnsChildren) {
            components.clear();
        }
    }
    
    @Override
    public Widget getView() {
        return view;
    }

    @Override
    public Dimension getMaxPreferredSize(Map<GContainer, GAbstractContainerView> containerViews) {
        int width = 0;
        int height = 0;
        for (int i = 0; i < columnsCount; ++i) {
            int columnHeight = 0;
            int columnWidth = 0;
            for (GComponent child : columnsChildren[i]) {
                if (getChildView(child).isVisible()) {
                    Dimension childPref = getChildMaxPreferredSize(containerViews, child);
                    columnHeight += childPref.height;
                    columnWidth = Math.max(columnWidth, childPref.width);
                }
            }
            
            if (columnWidth == 0) {
                //пустые колонки всё равно ренедрятся шириной в 1 пиксел
                columnWidth = 1;
            }
            
            width += columnWidth;
            height = Math.max(height, columnHeight);
        }
        return addCaptionDimensions(new Dimension(width, height));
    }

    private static final class ProxyPanel extends ResizableSimplePanel {
        private ProxyPanel(GComponent component, Widget child) {
            super(child);

            child.getElement().setPropertyObject(COLUMN_PROXY_KEY, this);

            component.installMargins(this);
        }
    }
}
