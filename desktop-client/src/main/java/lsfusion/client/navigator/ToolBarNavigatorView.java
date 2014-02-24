package lsfusion.client.navigator;

import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.FlexConstraints;
import lsfusion.interop.form.layout.FlexLayout;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class ToolBarNavigatorView extends NavigatorView {

    private final JToolBar toolBar;
    private final ClientToolBarNavigatorWindow window;
    private ClientNavigatorElement selected;

    public ToolBarNavigatorView(ClientToolBarNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JToolBar("Toolbar", iWindow.type), controller);
        window = iWindow;
        
        toolBar = (JToolBar) getComponent();
        toolBar.setLayout(new FlexLayout(toolBar, iWindow.type == SwingConstants.VERTICAL, Alignment.LEADING));
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

    private void addElement(ClientNavigatorElement element, Set<ClientNavigatorElement> newElements, int indent) {
        JComponent button = addNavigationButton(element, indent);
        if (window.showSelect && element.equals(getSelectedElement()) && (element.getClass() == ClientNavigatorElement.class)) {
            button.setForeground(Color.blue);
            button.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }
        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (ClientNavigatorElement childEl: element.children) {
            if (newElements.contains(childEl)) {
                addElement(childEl, newElements, indent + 1);
            }
        }
    }

    private JComponent addNavigationButton(ClientNavigatorElement element, final int indent) {
        JButton button = new JButton(element.toString());
        
        button.setToolTipText(element.toString());
        button.addMouseListener(new NavigatorMouseAdapter(element));
        button.setIcon(new IndentedIcon(element.image.getImage(), indent));
        button.setVerticalTextPosition(window.verticalTextPosition);
        button.setHorizontalTextPosition(window.horizontalTextPosition);
        button.setVerticalAlignment(window.verticalAlignment);
        button.setHorizontalAlignment(window.horizontalAlignment);
//        button.setAlignmentY(window.alignmentY);
//        button.setAlignmentX(window.alignmentX);
        button.setFocusable(false);

        toolBar.add(button, new FlexConstraints());
        
        return button;
    }

    @Override
    public ClientNavigatorElement getSelectedElement() {
        return selected;
    }

    public void setSelectedElement(ClientNavigatorElement element) {
        selected = element;
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

