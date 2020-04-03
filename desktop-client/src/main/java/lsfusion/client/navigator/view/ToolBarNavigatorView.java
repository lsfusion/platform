package lsfusion.client.navigator.view;

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
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import static javax.swing.BorderFactory.*;
import static lsfusion.client.base.view.SwingDefaults.getComponentBorderColor;
import static lsfusion.client.base.view.SwingDefaults.getSelectionColor;

public class ToolBarNavigatorView extends NavigatorView {
    private final Border NORMAL_BORDER = createEmptyBorder(3, 8, 3, 8);
    
    private final JToolBar toolBar;
    private final ClientToolBarNavigatorWindow window;
    private ClientNavigatorElement selected;

    public ToolBarNavigatorView(ClientToolBarNavigatorWindow iWindow, INavigatorController controller) {
        super(iWindow, new JToolBar("Toolbar", iWindow.type) {
            @Override
            public void updateUI() {
                super.updateUI();
            }
        }, controller);
        window = iWindow;

        toolBar = (JToolBar) getComponent();
        toolBar.setLayout(new FlexLayout(toolBar, iWindow.isVertical(), Alignment.START));
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
        JToggleButton button = new JToggleButton(element.toString()) {
            @Override
            public void updateUI() {
                super.updateUI();
                setIcon(new IndentedIcon(element.imageHolder.getImage(MainController.colorTheme), indent));
                if (isSelected()) {
                    setBorder(getSelectionBorder());
                }
            }
        };
        button.setIcon(new IndentedIcon(element.imageHolder.getImage(MainController.colorTheme), indent));

        button.setToolTipText(element.getTooltip());
        button.addMouseListener(new NavigatorMouseAdapter(element));
        button.setVerticalTextPosition(window.verticalTextPosition);
        button.setHorizontalTextPosition(window.horizontalTextPosition);
        button.setVerticalAlignment(window.verticalAlignment);
        button.setHorizontalAlignment(window.horizontalAlignment);
        button.setFocusable(false);
        button.getModel().setArmed(true);
        if (window.isVertical()) {
            button.setPreferredSize(new Dimension(button.getPreferredSize().width, 30));
        }
        
        button.setBorder(NORMAL_BORDER);

        button.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                button.setBorder(getSelectionBorder());
            } else {
                button.setBorder(NORMAL_BORDER);
            }
        });
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBorder(getSelectionBorder());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBorder(NORMAL_BORDER);
                }
            }
        });

        if (window.showSelect && element.equals(getSelectedElement()) && (element.getClass() == ClientNavigatorFolder.class)) {
            button.setSelected(true);
        }

        toolBar.add(button, new FlexConstraints(FlexAlignment.STRETCH, 0));

        if (element.window == null || element.window.equals(window)) {
            for (ClientNavigatorElement childEl : element.children) {
                if (newElements.contains(childEl)) {
                    addElement(childEl, newElements, indent + 1);
                }
            }
        }
    }
    
    private Border getSelectionBorder() {
        Border insidePaddingBorder = createEmptyBorder(2, 6, 2, 6);
        Border outsideComponetnBorder = createLineBorder(getComponentBorderColor());
        // for some reason 1px vertical lines without color (no background) are drawn with empty padding border only 
        MatteBorder insideBackgroundHackBorder = createMatteBorder(0, 1, 0, 1, getSelectionColor());
        return createCompoundBorder(createCompoundBorder(outsideComponetnBorder, insideBackgroundHackBorder), insidePaddingBorder);
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
            controller.openElement(getSelectedElement(), e.getModifiers());
        }
    }
}

