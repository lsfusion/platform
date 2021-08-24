package lsfusion.client.form.design.view;

import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;

import java.awt.*;
import java.util.Map;

public interface ClientContainerView {
    void add(ClientComponent child, FlexPanel view);
    void remove(ClientComponent child);

    boolean hasChild(ClientComponent child);
    int getChildrenCount();
    ClientComponent getChild(int index);
    Component getChildView(int index);

    FlexPanel getView();

    void updateCaption();

    void updateLayout();

    Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews);
}
