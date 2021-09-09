package lsfusion.client.form.design.view;

import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.widget.Widget;

import java.awt.*;
import java.util.Map;

public interface ClientContainerView {
    void add(ClientComponent child, Widget view);
    void remove(ClientComponent child);

    boolean hasChild(ClientComponent child);
    int getChildrenCount();
    ClientComponent getChild(int index);
    Widget getChildView(int index);

    Widget getView();

    void updateCaption(ClientContainer clientContainer);

    void updateLayout(boolean[] childrenVisible);

    Dimension getMaxPreferredSize(Map<ClientContainer, ClientContainerView> containerViews);
}
