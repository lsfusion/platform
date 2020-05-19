package lsfusion.client.form.design.view;

import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.interop.base.view.FlexConstraints;

public class LinearClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;

    public LinearClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);

        assert container.isLinear();
        panel = new ContainerViewPanel(container.isLinearVertical(), container.childrenAlignment);

        container.design.installFont(panel);
        ClientColorUtils.designComponent(panel, container.design);
    }

    @Override
    public void addImpl(int index, ClientComponent child, JComponentPanel view) {
//        view.setBorder(SwingUtils.randomBorder());

        FlexConstraints constraints = new FlexConstraints(child.getAlignment(), child.getFlex());
        add(panel, view, index, constraints, child);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, JComponentPanel view) {
        panel.remove(view);
    }

    @Override
    public ContainerViewPanel getPanel() {
        return panel;
    }
}
