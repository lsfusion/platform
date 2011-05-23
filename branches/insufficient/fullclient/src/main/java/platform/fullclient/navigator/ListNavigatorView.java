package platform.fullclient.navigator;

import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorWindow;
import platform.fullclient.layout.DockableMainFrame;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class ListNavigatorView extends NavigatorView {
    private JPanel panel;
    ClientNavigatorElement selected;

    public ListNavigatorView(ClientNavigatorWindow iWindow) {
        super(iWindow, new JPanel());
        panel = (JPanel) component;
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));


    }

    @Override
    public void refresh(Set<ClientNavigatorElement> newElements) {
        panel.removeAll();

        for (ClientNavigatorElement element : newElements) {
            JButton button = new JButton(element.toString());
            panel.add(button);
            Box.createHorizontalStrut(15);
            button.addMouseListener(new NavigatorMouseAdapter(element));
        }
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
        public void mouseClicked(MouseEvent e) {
            setSelectedElement(selected);
            DockableMainFrame.navigatorController.update(window, getSelectedElement());
            if (e.getClickCount() == 2) {
                DockableMainFrame.navigatorController.openForm(getSelectedElement());
            }
        }

    }
}

