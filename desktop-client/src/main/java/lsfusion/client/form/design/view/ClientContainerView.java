package lsfusion.client.form.design.view;

import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public interface ClientContainerView {
    void add(ClientComponent child, JComponent view);
    void remove(ClientComponent child);

    boolean hasChild(ClientComponent child);
    int getChildrenCount();
    ClientComponent getChild(int index);
    Component getChildView(int index);

    JComponent getView();

    void updateCaption(ClientContainer clientContainer);

    void updateLayout(boolean[] childrenVisible);

    Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews);
}
