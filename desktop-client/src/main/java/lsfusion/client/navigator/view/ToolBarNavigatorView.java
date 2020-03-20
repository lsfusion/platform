package lsfusion.client.navigator.view;

import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.client.base.view.ColorThemeChangeListener;
import lsfusion.client.controller.MainController;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.ClientNavigatorFolder;
import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.window.ClientToolBarNavigatorWindow;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;
import lsfusion.interop.base.view.FlexLayout;
import lsfusion.interop.form.design.Alignment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class ToolBarNavigatorView extends NavigatorView implements ColorThemeChangeListener {

    private final JToolBar toolBar;
    private final ClientToolBarNavigatorWindow window;
    private ClientNavigatorElement selected;

    public ToolBarNavigatorView(ClientToolBarNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JToolBar("Toolbar", iWindow.type), controller);
        window = iWindow;

        toolBar = (JToolBar) getComponent();
        toolBar.setLayout(new FlexLayout(toolBar, iWindow.type == SwingConstants.VERTICAL, Alignment.START));
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setFocusable(false);

        MainController.addColorThemeChangeListener(this);
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
        ToolbarNavigatorViewButton button = new ToolbarNavigatorViewButton(element, indent);

        toolBar.add(button, new FlexConstraints(FlexAlignment.STRETCH, 0));

        if (element.window == null || element.window.equals(window)) {
            for (ClientNavigatorElement childEl : element.children) {
                if (newElements.contains(childEl)) {
                    addElement(childEl, newElements, indent + 1);
                }
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

    @Override
    public void colorThemeChanged() {
        for (Component toolBarComponent : toolBar.getComponents()) {
            ((ToolbarNavigatorViewButton) toolBarComponent).updateIcon();    
        }
//        toolBar.updateUI();
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
            controller.openElement(getSelectedElement(), e.getModifiers());
        }
    }
    
    private class ToolbarNavigatorViewButton extends JToggleButton {
        private SerializableImageIconHolder imageHolder;
        private int indent;

        ToolbarNavigatorViewButton(ClientNavigatorElement element, int indent) {
            super(element.toString());
            this.imageHolder = element.imageHolder;
            this.indent = indent;

            updateIcon();

            setToolTipText(element.getTooltip());
            addMouseListener(new NavigatorMouseAdapter(element));
            setVerticalTextPosition(window.verticalTextPosition);
            setHorizontalTextPosition(window.horizontalTextPosition);
            setVerticalAlignment(window.verticalAlignment);
            setHorizontalAlignment(window.horizontalAlignment);
            setFocusable(false);
            getModel().setArmed(true);
            putClientProperty("Button.arc", "6");
            putClientProperty("Component.arc", "5");

            if (window.showSelect && element.equals(getSelectedElement()) && (element.getClass() == ClientNavigatorFolder.class)) {
//                setForeground(Color.blue);
                setSelected(true);
            }
        }
        
        public void updateIcon() {
            setIcon(new IndentedIcon(imageHolder.getImage(MainController.colorTheme), indent));
        }
    }
}

