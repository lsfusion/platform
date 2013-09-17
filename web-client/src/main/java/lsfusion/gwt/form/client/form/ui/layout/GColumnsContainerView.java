package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.ResizableComplexPanel;
import lsfusion.gwt.base.client.ui.ResizableSimplePanel;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;
import java.util.List;

public class GColumnsContainerView extends GAbstractContainerView {
    private final static String COLUMN_PROXY_KEY = "columnsProxy";

    private final int columnsCount;

    private final FlexPanel panel;

    //в идеале здесь должны быть FlexPanel'и, но пока слишком тормозит в FF
    //    private final FlexPanel[] columns;
    private final ResizableComplexPanel[] columns;

    private final List<GComponent>[] columnsChildren;

    private final Widget view;

    public GColumnsContainerView(GContainer container) {
        super(container);

        assert container.isColumns();

        panel = new FlexPanel();

        columnsCount = container.columns;

        columns = new ResizableComplexPanel[columnsCount];
        columnsChildren = new List[columnsCount];
        for (int i = 0; i < columnsCount; ++i) {
            ResizableComplexPanel column = new ResizableComplexPanel();
            panel.add(column);

            columns[i] = column;
            columnsChildren[i] = new ArrayList<GComponent>();
        }
        panel.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

        view = wrapWithCaptionAndSetMargins(panel);
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

        columnChildren.add(rowIndex, child);

        columns[colIndex].insert(new ProxyPanel(view), 2 * rowIndex);
        columns[colIndex].insert(new Clear(), 2 * rowIndex + 1);
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        panel.remove(view);
        int childIndex = container.children.indexOf(child);
        int colIndex = childIndex % columnsCount;
        columnsChildren[colIndex].remove(child);

        Object columnsProxy = view.getElement().getPropertyObject(COLUMN_PROXY_KEY);
        if (!(columnsProxy instanceof ProxyPanel)) {
            throw new IllegalStateException("Trying to delete something, that wasn't added");
        }

        ResizableComplexPanel column = columns[colIndex];
        int proxyIndex = column.getWidgetIndex((Widget) columnsProxy);

        column.remove(proxyIndex + 1);
        column.remove(proxyIndex);
    }

    @Override
    public Widget getView() {
        return view;
    }

    private static final class ProxyPanel extends ResizableSimplePanel {
        private ProxyPanel(Widget child) {
            setWidget(child);

            child.getElement().setPropertyObject(COLUMN_PROXY_KEY, this);

            getElement().getStyle().setFloat(Style.Float.LEFT);
        }
    }

    private static final class Clear extends Widget {
        private Clear() {
            DivElement divElement = Document.get().createDivElement();
            divElement.getStyle().setClear(Style.Clear.BOTH);

            setElement(divElement);
        }
    }
}
