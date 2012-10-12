package platform.client.form;

import platform.client.logics.ClientContainer;
import platform.interop.form.layout.ContainerType;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ClientFormSplitPane extends JSplitPane implements AutoHideableContainer {
    private LayoutManager2 layout;
    private boolean skipRelayout = false;

    public ClientFormSplitPane(ClientContainer key, LayoutManager2 layout, final ClientFormLayout formLayout) {
        super(key.getType() == ContainerType.SPLIT_PANE_HORIZONTAL ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, false);
        this.layout = layout;
        key.design.designComponent(this);
        setBorder(null);

        addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!skipRelayout) {
                    formLayout.dropCaches();
                }
            }
        });

        // вырезаем стандартные шорткаты
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "none");

        ((BasicSplitPaneUI) getUI()).getDivider().setBorder(BorderFactory.createEtchedBorder());
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

    public boolean areBothVisible() {
        return leftComponent != null && leftComponent.isVisible() && rightComponent != null && rightComponent.isVisible();
    }

    public void setDividerLocationSkipRelayout(int dividerLocation) {
        skipRelayout = true;
        setDividerLocation(dividerLocation);
        skipRelayout = false;
    }
}
