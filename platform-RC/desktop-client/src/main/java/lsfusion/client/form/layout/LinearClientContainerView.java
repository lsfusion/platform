package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.FlexLayout;
import lsfusion.interop.form.layout.FlexConstraints;

import javax.swing.*;
import java.awt.*;

public class LinearClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;

    public LinearClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isLinear();

        panel = new ContainerViewPanel();
        panel.setLayout(new FlexLayout(panel, container.isVertical(), container.childrenAlignment));

        container.design.designComponent(panel);
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
//        ((JComponent)view).setBorder(randomBorder());

        FlexConstraints constraints = new FlexConstraints(child.getAlignment(), child.getFlex());
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
