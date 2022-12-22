package lsfusion.client.navigator.view;

import com.formdev.flatlaf.ui.FlatButtonBorder;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.ToggleButtonWidget;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.ClientNavigatorFolder;
import lsfusion.client.navigator.controller.INavigatorController;
import lsfusion.client.navigator.window.ClientToolBarNavigatorWindow;
import lsfusion.client.tooltip.LSFTooltipManager;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import static javax.swing.BorderFactory.createEmptyBorder;
import static lsfusion.client.base.view.SwingDefaults.getSelectionColor;
import static lsfusion.client.base.view.SwingDefaults.getToggleButtonHoverBackground;

public class ToolBarNavigatorView extends NavigatorView {
    private final FlexPanel toolBarPanel;
    private final ClientToolBarNavigatorWindow window;
    private ClientNavigatorElement selected;

    public ToolBarNavigatorView(ClientToolBarNavigatorWindow window, INavigatorController controller) {
        super(window, new FlexPanel(window.isVertical(), window.getFlexAlignment()), controller);
        this.window = window;

        this.toolBarPanel = (FlexPanel) getComponent();
    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        toolBarPanel.removeAll();

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
        ToggleButtonWidget button = new ToggleButtonWidget(element.toString()) {
            @Override
            public void updateUI() {
                super.updateUI();
                ImageIcon icon = element.getImage();
                if (icon != null) {
                    setIcon(new IndentedIcon(icon, indent));
                }
                if (isSelected()) {
                    setBorder(getSelectionBorder());
                } else {
                    setBackground(null);
                }
            }
        };
        ImageIcon icon = element.getImage();
        if (icon != null) {
            button.setIcon(new IndentedIcon(icon, indent));
        }

        LSFTooltipManager.initTooltip(button, element.getTooltip(), element.path, element.creationPath);
        button.addMouseListener(new NavigatorMouseAdapter(element));
        button.setVerticalTextPosition(window.verticalTextPosition);
        button.setHorizontalTextPosition(window.horizontalTextPosition);
        button.setVerticalAlignment(window.verticalAlignment);
        button.setHorizontalAlignment(window.horizontalAlignment);
        button.setFocusable(false);
        button.getModel().setArmed(true);
        if (window.isVertical()) {
            button.setPreferredSize(new Dimension(button.getPreferredSize().width, SwingDefaults.getVerticalToolbarNavigatorButtonHeight()));
        }

        button.setBackground(null);
        button.setBorder(getNormalBorder());

        button.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                button.setBorder(getSelectionBorder());
            } else {
                button.setBorder(getNormalBorder());
            }
        });

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBorder(getSelectionBorder());
                }
                button.setBackground(getToggleButtonHoverBackground());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.isSelected()) {
                    button.setBorder(getNormalBorder());
                    button.setBackground(null);
                } else {
                    button.setBackground(getSelectionColor());
                }
            }
        });

        if (window.showSelect && element.equals(getSelectedElement()) && (element.getClass() == ClientNavigatorFolder.class)) {
            button.setSelected(true);
        }

        toolBarPanel.add(button, FlexAlignment.STRETCH, 0.0);

        if (element.window == null || element.window.equals(window)) {
            for (ClientNavigatorElement childEl : element.children) {
                if (newElements.contains(childEl)) {
                    addElement(childEl, newElements, indent + 1);
                }
            }
        }
    }
    
    private Border getNormalBorder() {
        Insets insets = SwingDefaults.getToggleButtonMargin();
        return createEmptyBorder(insets.top + 1, insets.left + 1, insets.bottom + 1, insets.right + 1);
    }

    private Border getSelectionBorder() {
        return new FlatButtonBorder();
    }

    @Override
    public ClientNavigatorElement getSelectedElement() {
        return selected;
    }

    public void setSelectedElement(ClientNavigatorElement element) {
        selected = element;
    }

    @Override
    public void resetSelectedElement(ClientNavigatorElement newSelectedElement) {
        ClientNavigatorElement selectedElement = getSelectedElement();
        if(selectedElement != null && selectedElement.findChild(newSelectedElement) == null) {
            setSelectedElement(null);
        }
    }

    private class NavigatorMouseAdapter extends MouseAdapter {
        ClientNavigatorElement selected;

        public NavigatorMouseAdapter(ClientNavigatorElement element) {
            this.selected = element;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            controller.resetSelectedElements(selected);
            setSelectedElement(selected);
            controller.update();
            controller.openElement(getSelectedElement(), e.getModifiers());
        }
    }
}

