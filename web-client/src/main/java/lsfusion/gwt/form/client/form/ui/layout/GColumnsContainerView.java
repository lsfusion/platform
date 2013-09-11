package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;
import java.util.List;

public class GColumnsContainerView extends GAbstractContainerView {
    private final FlexPanel panel;

    private final Widget view;

    private final int columnsCount;
    private final FlexPanel[] columns;
    private final List<GComponent>[] columnsChildren;

    public GColumnsContainerView(GContainer container) {
        super(container);

        assert container.isColumns();

        panel = new FlexPanel(false, FlexPanel.Justify.LEADING);

        columnsCount = container.columns;

        columns = new FlexPanel[columnsCount];
        columnsChildren = new List[columnsCount];
        for (int i = 0; i < columnsCount; ++i) {
            FlexPanel column = new FlexPanel(true, FlexPanel.Justify.LEADING);
            panel.add(column, GFlexAlignment.STRETCH, 0);

            columns[i] = column;
            columnsChildren[i] = new ArrayList<GComponent>();
        }
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = wrapWithCaption(panel);
    }

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

//        columns[colIndex].add(view, GFlexAlignment.STRETCH, 0);
        columns[colIndex].add(view, rowIndex, GFlexAlignment.LEADING, 0);
        columnChildren.add(rowIndex, child);
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        panel.remove(view);
        int childIndex = container.children.indexOf(child);
        int colIndex = childIndex % columnsCount;
        columnsChildren[colIndex].remove(child);
        columns[colIndex].remove(view);

//        int childIndex = container.children.indexOf(child);
//        int colIndex = childIndex % columnsCount;
//        columns[colIndex].remove(view);

//        view.removeFromParent();
    }

    @Override
    public Widget getView() {
        return view;
    }
}
