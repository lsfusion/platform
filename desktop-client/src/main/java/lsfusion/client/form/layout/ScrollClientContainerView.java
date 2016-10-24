package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;

import javax.swing.*;
import java.awt.*;

public class ScrollClientContainerView extends AbstractClientContainerView {

    private final JScrollPane scroll;

    public ScrollClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isScroll();
        scroll = new JScrollPane();
        container.design.designComponent(scroll);
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
        view.setPreferredSize(child.preferredSize);
        scroll.setViewportView(view);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, Component view) {
        scroll.getViewport().setView(null);
    }

    @Override
    public JComponent getView() {
        return scroll;
    }

    @Override
    public void updateLayout() {
        super.updateLayout();
        if(container.preferredSize != null) {
            int width = container.preferredSize.width > 0 ? container.preferredSize.width : scroll.getPreferredSize().width;
            int height = container.preferredSize.height > 0 ? container.preferredSize.height : scroll.getPreferredSize().height;
            scroll.setPreferredSize(new Dimension(width, height));
        }
    }
}
