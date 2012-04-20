package platform.client.form;

import platform.client.logics.ClientContainer;
import platform.interop.form.layout.ContainerType;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

public class ClientFormSplitPane extends JSplitPane implements AutoHideableContainer {
    private LayoutManager2 layout;

    public ClientFormSplitPane(ClientContainer key, LayoutManager2 layout, final ClientFormLayout formLayout) {
        super(key.getType() == ContainerType.SPLIT_PANE_HORIZONTAL ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, false);
        this.layout = layout;
        key.design.designComponent(this);
        setBorder(null);

        addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                formLayout.dropCaches();
            }
        });
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (constraints != null && !(constraints instanceof String)) {
            SimplexLayout.showHideableContainers(this);
            layout.addLayoutComponent(comp, constraints);
            if (leftComponent == null) {
                setLeftComponent(comp);
            } else {
                setRightComponent(comp);
            }
        } else {
            super.addImpl(comp, constraints, index);
        }
    }
}
