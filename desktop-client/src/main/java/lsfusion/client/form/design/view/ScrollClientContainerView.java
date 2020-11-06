package lsfusion.client.form.design.view;

import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;
import java.awt.*;

public class ScrollClientContainerView extends AbstractClientContainerView {

    private final ContainerViewPanel panel;
    private final JScrollPane scroll;

    public ScrollClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isScroll();
        
        scroll = new JScrollPane() {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(null); // is set on every color theme change in installDefaults()
            }
        };
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getHorizontalScrollBar().setUnitIncrement(14);
        ClientColorUtils.designComponent(scroll, container.design);

        panel = new ContainerViewPanel();
        // to forward a mouse wheel event in nested scroll pane to the parent scroll pane
        JLayer<JScrollPane> scrollLayer = new JLayer<>(scroll, new MouseWheelScrollLayerUI());
        panel.add(scrollLayer, BorderLayout.CENTER);
        setSizes(panel, container);
    }

    @Override
    public void addImpl(int index, ClientComponent child, JComponentPanel view) {
        assert child.getFlex() == 1 && child.getAlignment() == FlexAlignment.STRETCH; // временные assert'ы чтобы проверить обратную совместимость
        scroll.setViewportView(view);
        setSizes(view, child);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, JComponentPanel view) {
        scroll.getViewport().setView(null);
    }

    @Override
    public ContainerViewPanel getPanel() {
        return panel;
    }
}
