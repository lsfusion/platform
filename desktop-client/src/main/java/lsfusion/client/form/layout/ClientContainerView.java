package lsfusion.client.form.layout;

import lsfusion.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public interface ClientContainerView {
    public void add(ClientComponent child, Component view);
    public void remove(ClientComponent child);

    public boolean hasChild(ClientComponent child);
    public int getChildrenCount();
    public ClientComponent getChild(int index);
    public Component getChildView(int index);

    public JComponent getView();

    public void updateLayout();
}
