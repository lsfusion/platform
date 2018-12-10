package lsfusion.gwt.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.ui.FlexPanel;
import lsfusion.gwt.shared.view.GFlexAlignment;
import lsfusion.gwt.client.base.ui.ResizableSimplePanel;
import lsfusion.gwt.shared.view.GComponent;
import lsfusion.gwt.shared.view.GContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ColumnsContainerView<P extends Panel> extends GAbstractContainerView {
    protected final static String COLUMN_PROXY_KEY = "columnsProxy";

    protected final int columnsCount;

    protected final P panel;

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

        view = wrapWithCaption(panel);
    }
    
    private double getColumnFlex(GContainer container) {
        GContainer container2 = container.container;
        if (container2 == null || !container2.isHorizontal()) {
            return container.getAlignment() == GFlexAlignment.STRETCH ? 1 : 0;
        }
        return container.getFlex();
    }

    protected abstract P createHorizontalPanel();
    protected abstract Widget wrapWithCaption(P panel);

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
//        panel.add(view, index, child.getFlexAlignment(false), child.getFlexFill(false));

        int childCount = container.children.size();
        int childIndex = container.children.indexOf(child);
        int colIndex = childIndex % columnsCount;
        int rowIndex = 0;

        List<GComponent> columnChildren = columnsChildren[colIndex];
        if (columnChildren.size() != 0) {
            int currentRowIndex = 0;
            GComponent currentRowChild = columnChildren.get(0);
            for (int i = colIndex; i < childCount; i += columnsCount) {
                GComponent existingChild = container.children.get(i);
                if (existingChild == child) {
                    rowIndex = currentRowIndex;
                    break;
                }
                if (currentRowChild == existingChild) {
                    currentRowIndex++;
                    if (currentRowIndex == columnChildren.size()) {
                        rowIndex = currentRowIndex;
                        break;
                    }
                    currentRowChild = columnChildren.get(currentRowIndex);
                }
            }
        }

        columnChildren.add(rowIndex, child);

        columns[colIndex].add(new ProxyPanel(child, view), child.getAlignment());
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        int childIndex = container.children.indexOf(child);
        int colIndex = childIndex % columnsCount;
        columnsChildren[colIndex].remove(child);

        Object columnsProxy = view.getElement().getPropertyObject(COLUMN_PROXY_KEY);
        if (!(columnsProxy instanceof ProxyPanel)) {
            throw new IllegalStateException("Trying to delete something, that wasn't added");
        }

        FlexPanel column = columns[colIndex];
        int proxyIndex = column.getWidgetIndex((Widget) columnsProxy);

        column.remove(proxyIndex);
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

            component.installPaddings(this);
        }
    }
}
