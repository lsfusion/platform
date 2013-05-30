package platform.client.navigator;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class ToolBarNavigatorView extends NavigatorView {

    private static final int X_ALLIGN = 15;

    private JToolBar toolBar;
    private ClientNavigatorElement selected;
    private ClientToolBarNavigatorWindow window;

    public ToolBarNavigatorView(ClientToolBarNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JToolBar("Toolbar", iWindow.type), controller);
        window = iWindow;
        toolBar = (JToolBar) getComponent();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setFocusable(false);
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        toolBar.removeAll();

        for (ClientNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, newElements, 0);
            }
        }

        component.revalidate();
        component.repaint();

        // затычка, иначе Toolbar вверху рисуется неправильного размера (увеличенного)
        component.setPreferredSize(component.getPreferredSize());
    }

    private void addElement(ClientNavigatorElement element, Set<ClientNavigatorElement> newElements, int allign) {
        JComponent button = addNavigationButton(element, allign);
        if (window.showSelect && element.equals(getSelectedElement()) && (element.getClass() == ClientNavigatorElement.class)) {
            button.setForeground(Color.blue);
            button.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }
        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (ClientNavigatorElement childEl: element.children) {
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

    private JComponent addNavigationButton(ClientNavigatorElement element, final int align) {

        JButton button = new JButton(element.toString()) {
            @Override
            public Insets getInsets() {
                return new Insets(4, 4 + X_ALLIGN * align, 4, 4);
            }
        };

        button.setToolTipText(element.toString());
        button.addMouseListener(new NavigatorMouseAdapter(element));
        button.setIcon(element.image.getImage());
        button.setVerticalTextPosition(window.verticalTextPosition);
        button.setHorizontalTextPosition(window.horizontalTextPosition);
        button.setVerticalAlignment(window.verticalAlignment);
        button.setHorizontalAlignment(window.horizontalAlignment);
        button.setAlignmentY(window.alignmentY);
        button.setAlignmentX(window.alignmentX);
        button.setFocusable(false);

        // пока неактуально - лучше чтобы она красиво рисовала кнопки, чем отступы слева
//        if (window.type == JToolBar.VERTICAL) {
//            JPanel pane = new JPanel();
//            pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
//            pane.add(Box.createHorizontalStrut(X_ALLIGN * allign));
//            pane.add(button);
//            button.setRolloverEnabled(true);
//            button.setPreferredSize(new Dimension(getMinimumSize().width, getMinimumSize().height));
//            button.setMaximumSize(new Dimension(BUTTON_WIDTH, getMinimumSize().height));
//            pane.setMaximumSize(button.getMaximumSize());
//            comp = pane;
//        }
        toolBar.add(button);
//        toolBar.setPreferredSize(new Dimension(toolBar.getPreferredSize().width, button.getPreferredSize().height));
        return button;
    }

    private class NavigatorMouseAdapter extends MouseAdapter {
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

