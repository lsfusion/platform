package lsfusion.client.form.design.view.widget;

import javax.swing.*;
import java.awt.*;

import static lsfusion.client.base.SwingUtils.overrideSize;

public class SeparatorWidget extends JSeparator implements Widget {

    public SeparatorWidget(int orientation) {
        super(orientation);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    private Dimension componentSize = new Dimension(-1, -1);

    public Dimension getComponentSize() {
        return componentSize;
    }

    public void setComponentSize(Dimension componentSize) {
        this.componentSize = componentSize;
    }

    @Override
    public Dimension getPreferredSize() {
        return overrideSize(super.getPreferredSize(), getComponentSize());
    }

    public Object debugContainer;

    @Override
    public Object getDebugContainer() {
        return debugContainer;
    }

    @Override
    public void setDebugContainer(Object debugContainer) {
        this.debugContainer = debugContainer;
    }

    @Override
    public String toString() {
        return (debugContainer != null ? debugContainer + " " : "") + super.toString();
    }
}
