package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ColumnsClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;
    private final int columnsCount;
    private final JPanel[] columns;
    private final List<ClientComponent>[] columnsChildren;

    public ColumnsClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isColumns();

        panel = new ContainerViewPanel();
        panel.setLayout(new FlexLayout(panel, false, Alignment.LEADING));

        columnsCount = container.columns;

        columns = new JPanel[columnsCount];
        columnsChildren = new List[columnsCount];
        for (int i = 0; i < columnsCount; ++i) {
            JPanel column = new JPanel();
            column.setLayout(new ColumnsLayout(column, 1));
            panel.add(column, new FlexConstraints());

            columns[i] = column;
            columnsChildren[i] = new ArrayList<ClientComponent>();
        }

        container.design.designComponent(panel);
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
//        ((JComponent)view).setBorder(randomBorder());

//        panel.add(view, new ColumnsConstraints(child.getAlignment()), index);

        int childCount = container.children.size();
        int childIndex = container.children.indexOf(child);
        int colIndex = childIndex % columnsCount;
        int rowIndex = 0;

        List<ClientComponent> columnChildren = columnsChildren[colIndex];
        if (columnChildren.size() != 0) {
            int currentRowIndex = 0;
            ClientComponent currentRowChild = columnChildren.get(0);
            for (int i = colIndex; i < childCount; i += columnsCount) {
                ClientComponent existingChild = container.children.get(i);
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

//        columns[colIndex].add(view, new FlexConstraints(FlexAlignment.STRETCH, 0), rowIndex);
        columns[colIndex].add(view, new ColumnsConstraints(FlexAlignment.STRETCH), rowIndex);
        columnChildren.add(rowIndex, child);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, Component view) {
        int childIndex = container.children.indexOf(child);
        int colIndex = childIndex % columnsCount;
        columnsChildren[colIndex].remove(child);
        columns[colIndex].remove(view);

//        panel.remove(view);
    }

    @Override
    public JComponent getView() {
        return panel;
    }
}
