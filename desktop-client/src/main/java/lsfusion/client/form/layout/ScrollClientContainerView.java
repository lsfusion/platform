package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;

import javax.swing.*;
import java.awt.*;

public class ScrollClientContainerView extends AbstractClientContainerView {

    private final Scroll scroll;
    private final ContainerViewPanel scrollPanel;

    public ScrollClientContainerView(ClientFormLayout formLayout, ClientContainer container) {
        super(formLayout, container);
        assert container.isColumns();

        scroll = new Scroll();
        scrollPanel = new ContainerViewPanel(new BorderLayout());
        scrollPanel.add(scroll);

        container.design.designComponent(scroll);
    }

    @Override
    public void addImpl(int index, ClientComponent child, Component view) {
        scroll.setViewportView(view);
    }

    @Override
    public void removeImpl(int index, ClientComponent child, Component view) {
        assert scroll.getViewport() != null;

        scroll.getViewport().setView(null);
    }

    @Override
    public JComponent getView() {
        return scrollPanel;
    }

    private class Scroll extends JScrollPane {
        public Scroll() {
            super(null);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public boolean isValidateRoot() {
            //не останавливаемся в поиске validate-root, а идём дальше вверх до верхнего контейнера
            return false;
        }
    }
}
