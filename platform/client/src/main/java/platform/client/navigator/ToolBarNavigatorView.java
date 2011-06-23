package platform.client.navigator;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class ToolBarNavigatorView extends NavigatorView {

    public static final int X_ALLIGN = 15;
    public static final int BUTTON_WIDTH = 400;
    private JToolBar toolBar;
    ClientNavigatorElement selected;
    ClientToolBarNavigatorWindow window;

    public ToolBarNavigatorView(ClientToolBarNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JToolBar("Toolbar", iWindow.type), controller);
        window = iWindow;
        toolBar = (JToolBar) component;
        toolBar.setFloatable(true);
        toolBar.setRollover(true);
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        toolBar.removeAll();

        for (ClientNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, newElements, 0);
            }
        }

        revalidate();
        repaint();
//        toolBar.revalidate();
    }

    private void addElement(ClientNavigatorElement element, Set<ClientNavigatorElement> newElements, int allign) {
        JComponent button = addNavigationButton(element, allign);
        if (window.showSelect && element.equals(getSelectedElement()) && ! (element instanceof ClientNavigatorForm)) {
            button.setForeground(Color.blue);
            button.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }
        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (ClientNavigatorElement childEl: element.childrens) {
            if (newElements.contains(childEl)) {
                addElement(childEl, newElements, allign + 1);
            }
        }
    }

    @Override
    public ClientNavigatorElement getSelectedElement() {
        return selected;
    }

    public void setSelectedElement(ClientNavigatorElement element) {
        selected = element;
    }

    protected JComponent addNavigationButton(ClientNavigatorElement element, int allign) {
        JButton button = new JButton(element.toString()) {
            @Override
            public Insets getInsets() {
                return new Insets(5, 7, 5, 7);
            }
        };
        JComponent comp = button;
        button.setToolTipText(element.toString());
        button.addMouseListener(new NavigatorMouseAdapter(element));
        button.setIcon(element.image);
        button.setVerticalTextPosition(window.verticalTextPosition);
        button.setHorizontalTextPosition(window.horizontalTextPosition);
        if (window.type == JToolBar.VERTICAL) {
            button.setHorizontalAlignment(SwingConstants.LEFT);
            JPanel pane = new JPanel();
            pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
            pane.add(Box.createHorizontalStrut(X_ALLIGN * allign));
            pane.add(button);
            button.setPreferredSize(new Dimension(getMinimumSize().width, getMinimumSize().height));
            button.setMaximumSize(new Dimension(BUTTON_WIDTH, getMinimumSize().height));
            pane.setMaximumSize(button.getMaximumSize());
            comp = pane;
        }
        toolBar.add(comp);
        return comp;
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
            controller.openForm(getSelectedElement());
        }
    }
}

