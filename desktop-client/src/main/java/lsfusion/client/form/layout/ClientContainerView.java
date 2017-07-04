package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;

import java.awt.*;

public interface ClientContainerView {
    void add(ClientComponent child, JComponentPanel view);
    void remove(ClientComponent child);

    boolean hasChild(ClientComponent child);
    int getChildrenCount();
    ClientComponent getChild(int index);
    Component getChildView(int index);

    JComponentPanel getView();

    void updateLayout();
}
