package platform.client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


public class FormFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
    private ArrayList<Component> list;

    public FormFocusTraversalPolicy() {
        list = new ArrayList<Component>();
    }

    @Override
    public Component getDefaultComponent(Container aContainer) {
        if (list.size() == 0) {
            return super.getDefaultComponent(aContainer);
        }
        Component c = list.get(0);
        if (c instanceof Container) {
            return super.getDefaultComponent((Container) c);
        } else {
            return c;
        }
    }

    public void addDefault(Component c) {
        list.add(c);
    }

    public void removeDefault(Component c) {
        list.remove(c);
    }
}
