package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.interop.form.layout.FlexLayout;
import lsfusion.interop.form.layout.FlexConstraints;

public class LinearClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;

    public LinearClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);

        assert container.isLinear();
        panel = new ContainerViewPanel(container.isLinearVertical(), container.childrenAlignment);

        container.design.designComponent(panel);
    }

    @Override
    public void addImpl(int index, ClientComponent child, JComponentPanel view) {
//        ((JComponent)view).setBorder(randomBorder());

        FlexConstraints constraints = new FlexConstraints(child.getAlignment(), child.getFlex());
        add(panel, view, index, constraints, child);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, JComponentPanel view) {
        panel.remove(view);
    }

    @Override
    public JComponentPanel getView() {
        return panel;
    }
}
