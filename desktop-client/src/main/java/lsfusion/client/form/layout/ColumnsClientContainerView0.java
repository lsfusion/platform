package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.ColumnsConstraints;
import lsfusion.interop.form.layout.ColumnsLayout;

import javax.swing.*;
import java.awt.*;

//todo: use or remove
public class ColumnsClientContainerView0 extends AbstractClientContainerView {

    private final ContainerViewPanel panel;

    public ColumnsClientContainerView0(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isColumns();

        panel = new ContainerViewPanel();
        panel.setLayout(new ColumnsLayout(panel, container.columns, container.gapX, container.gapY));

        container.design.designComponent(panel);
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
//        ((JComponent)view).setBorder(randomBorder());

        ColumnsConstraints constraints = new ColumnsConstraints(child.getAlignment());
        panel.add(view, constraints, index);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, Component view) {
        panel.remove(view);
    }

    @Override
    public JComponent getView() {
        return panel;
    }
}
