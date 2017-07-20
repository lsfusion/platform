package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public interface ClientContainerView {
    void add(ClientComponent child, Component view);
    void remove(ClientComponent child);

    boolean hasChild(ClientComponent child);
    int getChildrenCount();
    ClientComponent getChild(int index);
    Component getChildView(int index);

    JComponent getView();

    void updateLayout();
}
