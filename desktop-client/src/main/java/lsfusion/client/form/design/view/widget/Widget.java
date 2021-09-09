package lsfusion.client.form.design.view.widget;

import javax.swing.*;
import java.awt.*;

public interface Widget {

    JComponent getComponent();

    default boolean isVisible() {
        return getComponent().isVisible();
    }
    default void setVisible(boolean isVisible) {
        getComponent().setVisible(isVisible);
    }
    default Component getParent() {
        return getComponent().getParent();
    }

    Dimension getComponentSize();
    void setComponentSize(Dimension componentSize);
    Dimension getPreferredSize();
    default Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }

    Object getDebugContainer();
    void setDebugContainer(Object debugContainer);
}
