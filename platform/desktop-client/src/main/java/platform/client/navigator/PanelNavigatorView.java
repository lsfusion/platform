package platform.client.navigator;

import platform.client.FlatRolloverButton;
import platform.client.descriptor.editor.base.TitledPanel;
import sun.awt.OrientableFlowLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class PanelNavigatorView extends NavigatorView {
    ClientNavigatorElement selected;
    JPanel panel;
    int orientation;

    public PanelNavigatorView(ClientPanelNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JPanel(), controller);
        panel = (JPanel) getComponent();
        orientation = iWindow.orientation;
        panel.setLayout(new BoxLayout(panel, orientation == SwingConstants.VERTICAL ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        panel.removeAll();
        for (ClientNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, panel);
            }
        }
    }

    private void addElement(ClientNavigatorElement element, JPanel container) {
        TitledPanel titledPanel = new TitledPanel(element.toString(), new BorderLayout()) {
            @Override
            public Insets getInsets() {
                return new Insets(16, 4, 4, 4);
            }
        };

        //оборачиваем в панель, потому что OrientableFlowLayout не признаёт Border'ов панели
        JPanel insidePanel = new JPanel(new OrientableFlowLayout(OrientableFlowLayout.VERTICAL, OrientableFlowLayout.LEFT, OrientableFlowLayout.TOP, 0, 0, 0, 0));

        if (element instanceof ClientNavigatorForm) {
            insidePanel.add(createButton(element));
        } else {
            for (ClientNavigatorElement child : element.children) {
                if (child.hasChildren()) {
                    addElement(child, insidePanel);
                } else {
                    insidePanel.add(createButton(child));
                }
            }
        }

        adjustPreferredSizes(insidePanel); //чтобы отрисовать кнопки и заоднопроставить им равные ширины
        Component childComponent = insidePanel.getComponent(0);
        insidePanel.setPreferredSize(new Dimension(childComponent.getPreferredSize().width * insidePanel.getComponentCount(), childComponent.getPreferredSize().height));
        insidePanel.setMinimumSize(childComponent.getMinimumSize());

        titledPanel.add(insidePanel, BorderLayout.CENTER);
        container.add(titledPanel);
    }

    private void adjustPreferredSizes(JPanel container) {
        for (Component component : container.getComponents()) {
            component.setPreferredSize(new Dimension(container.getPreferredSize().width, component.getPreferredSize().height));
        }
    }

    private JButton createButton(ClientNavigatorElement element) {
        JButton button = new FlatRolloverButton(element.toString()) {
            @Override
            public Insets getInsets() {
                return new Insets(4, 4, 4, 4);
            }
        };
        button.setIcon(element.image.getImage());
        button.setToolTipText(element.toString());
        button.addMouseListener(new NavigatorMouseAdapter(element));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusable(false);
        return button;
    }

    @Override
    public ClientNavigatorElement getSelectedElement() {
        return selected;
    }

    public void setSelectedElement(ClientNavigatorElement element) {
        selected = element;
    }

    class NavigatorMouseAdapter extends MouseAdapter {
        ClientNavigatorElement selected;

        public NavigatorMouseAdapter(ClientNavigatorElement element) {
            this.selected = element;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            setSelectedElement(selected);
            controller.update();
            controller.openElement(getSelectedElement());
        }
    }
}
