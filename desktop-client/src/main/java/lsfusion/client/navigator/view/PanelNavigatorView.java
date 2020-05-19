package lsfusion.client.navigator.view;

import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.FlatRolloverButton;
import lsfusion.client.form.object.table.grid.user.toolbar.view.TitledPanel;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.window.ClientPanelNavigatorWindow;
import org.jdesktop.swingx.VerticalLayout;

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
        TitledPanel titledPanel = new TitledPanel(element.toString()) {
            @Override
            public Insets getInsets() {
                return new Insets(16, 4, 4, 4);
            }
        };

        JPanel insidePanel = new JPanel(new VerticalLayout());

        for (ClientNavigatorElement child : element.children) {
            if (child.hasChildren()) {
                addElement(child, insidePanel);
            } else {
                insidePanel.add(createButton(child));
            }
        }

        adjustPreferredSizes(insidePanel); // растягиваем child'ы на ширину контейнера

        titledPanel.add(insidePanel);
        container.add(titledPanel);
    }

    private void adjustPreferredSizes(JPanel container) {
        for (Component component : container.getComponents()) {
            component.setPreferredSize(new Dimension(container.getPreferredSize().width, component.getPreferredSize().height));
        }
    }

    private JButton createButton(ClientNavigatorElement element) {
        JButton button = new FlatRolloverButton(ClientImages.getImage(element.imageHolder), element.toString()) {
            @Override
            public Insets getInsets() {
                return new Insets(4, 4, 4, 4);
            }
        };
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
            controller.openElement(getSelectedElement(), e.getModifiers());
        }
    }
}
