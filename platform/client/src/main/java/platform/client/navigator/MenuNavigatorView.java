package platform.client.navigator;

import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorWindow;
import platform.client.navigator.NavigatorView;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

public class MenuNavigatorView extends NavigatorView {
    JMenuBar menuBar;
    ClientMenuNavigatorWindow window;
    ClientNavigatorElement selected;

    public MenuNavigatorView(ClientMenuNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JMenuBar(), controller);
        menuBar = (JMenuBar) component;
        window = iWindow;
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        menuBar.removeAll();
        for (ClientNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                JComponent component = addElement(menuBar, element, newElements, 0);
            }
        }
    }

    private JComponent addElement(JComponent parent, ClientNavigatorElement element, Set<ClientNavigatorElement> newElements, int level) {
        boolean isLeaf = true;
        for (ClientNavigatorElement childEl: element.childrens) {
            if (newElements.contains(childEl)) {
                isLeaf = false;
                break;
            }
        }

        JComponent node;
        if (isLeaf || ((element.window != null) && (!element.window.equals(window)))) {
            node = addLeaf(parent, element);
        } else {

            if (level < window.showLevel) {
                node = parent;
            } else {
                node = addNode(parent, element);
            }
            for (ClientNavigatorElement childEl: element.childrens) {
                if (newElements.contains(childEl)) {
                    addElement(node, childEl, newElements, level + 1);
                }
            }
        }
        if (element.equals(getSelectedElement())) {
            node.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }
        return node;
    }

    private JMenuItem addLeaf(JComponent parent, ClientNavigatorElement element) {
        JMenuItem menuItem = new JMenuItem(element.toString());
        menuItem.setIcon(element.image);
        menuItem.addActionListener(new MenuActionListener(element));
        parent.add(menuItem);
        return menuItem;
    }

    private JMenu addNode(JComponent parent, ClientNavigatorElement element) {
        JMenu menu = new JMenu(element.toString());
        menu.setIcon(element.image);
        parent.add(menu);
        return menu;
    }

    @Override
    public ClientNavigatorElement getSelectedElement() {
        return selected;
    }

    private class MenuActionListener implements ActionListener {
        private ClientNavigatorElement element;

        public MenuActionListener(ClientNavigatorElement element) {
            this.element = element;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selected = element;
            controller.update(window, getSelectedElement());
            controller.openForm(getSelectedElement());
        }
    }
}
