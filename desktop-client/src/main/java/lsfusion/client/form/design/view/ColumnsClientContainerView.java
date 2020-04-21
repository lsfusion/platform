package lsfusion.client.form.design.view;

import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;
import lsfusion.interop.form.design.Alignment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ColumnsClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;
    private final int columnsCount;
    private final JPanel[] columns;
    private final List<ClientComponent>[] columnsChildren;

    public ColumnsClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isColumns();

        panel = new ContainerViewPanel(false, Alignment.START);

        columnsCount = container.columns;

        columns = new JPanel[columnsCount];
        columnsChildren = new List[columnsCount];
        double columnFlex = getColumnFlex(container);
        for (int i = 0; i < columnsCount; ++i) {
            JPanel column = new JPanel();
            column.setLayout(new FlexLayout(column, true, Alignment.START));
            column.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
            panel.add(column, new FlexConstraints(FlexAlignment.START, columnFlex));

            columns[i] = column;
            columnsChildren[i] = new ArrayList<>();
        }

        container.design.designComponent(panel);
    }

    private double getColumnFlex(ClientContainer container) {
        ClientContainer container2 = container.container;
        if (container2 == null || !container2.isHorizontal()) {
            return container.getAlignment() == FlexAlignment.STRETCH ? 1 : 0;
        }
        return container.getFlex();
    }

    @Override
    public Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews) {
        int width = 0;
        int height = 0;
        for (int i = 0; i < columnsCount; ++i) {
            int columnHeight = 0;
            int columnWidth = 0;
            for (ClientComponent child : columnsChildren[i]) {
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

    @Override
    public void addImpl(int index, ClientComponent child, JComponentPanel view) {
        rebuildColumnCollections();
    }

    @Override
    public void removeImpl(int index, ClientComponent child, JComponentPanel view) {
        rebuildColumnCollections();
    }

    private void rebuildColumnCollections() {
        clearColumnsCollections();
        
        int cnt = 0;
        for (ClientComponent child : container.children) {
            int index = children.indexOf(child);
            if (index >= 0) {
                int colIndex = cnt % columnsCount;
                int rowIndex = cnt / columnsCount;
                columns[colIndex].add(childrenViews.get(index), new FlexConstraints(child.getAlignment(), 0), rowIndex);
                columnsChildren[colIndex].add(rowIndex, child);
                ++cnt;
            }
        }
    }
    
    private void clearColumnsCollections() {
        for (JPanel panel : columns) {
            panel.removeAll();
        }
        
        for (List<ClientComponent> components : columnsChildren) {
            components.clear();
        }
    }

    @Override
    public ContainerViewPanel getPanel() {
        return panel;
    }
}
