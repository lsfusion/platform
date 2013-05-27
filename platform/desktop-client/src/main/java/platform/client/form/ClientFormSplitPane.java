package platform.client.form;

import platform.client.logics.ClientContainer;
import platform.interop.form.layout.ContainerType;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;

import static platform.client.SwingUtils.getNewBoundsIfNotAlmostEquals;

public class ClientFormSplitPane extends JSplitPane implements AutoHideableContainer {
    private LayoutManager2 layout;
    private boolean skipRelayout = false;

    private Field ignoreLocationChangeField;

    public ClientFormSplitPane(ClientContainer key, LayoutManager2 layout, final ClientFormLayout formLayout) {
        super(key.getType() == ContainerType.SPLIT_PANE_HORIZONTAL ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, false);
        this.layout = layout;
        key.design.designComponent(this);
        setBorder(null);

        try {
            ignoreLocationChangeField = BasicSplitPaneUI.class.getDeclaredField("ignoreDividerLocationChange");
        } catch (NoSuchFieldException e) {
        }

        addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // Основная проблема метода заключается в том, что setDividerLocation вызывается в paint, то есть после выполнения Layout
                boolean ignoreLocationChange = false;
                try {
                    // Очень хитрый хак, иначе происходит рекурсия при изменении dividerLocation и форма начинает "плыть"
                    ignoreLocationChange = ignoreLocationChangeField != null && (Boolean)ignoreLocationChangeField.get(ClientFormSplitPane.this);
                } catch (IllegalAccessException e) {
                }

                // Не сбрасываем кэши, если очень маленькое изменение (иначе иногда возникает цикл на перерисовке)
                if (evt.getOldValue() instanceof Integer && evt.getNewValue() instanceof Integer &&
                    (Math.abs((Integer)evt.getOldValue() - (Integer)evt.getNewValue()) <= 1))
                    ignoreLocationChange = true;

                if (!skipRelayout && !ignoreLocationChange) {
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

    //Чтобы лэйаут не прыгал игнорируем мелкие изменения координат
    @Override
    public void setBounds(int x, int y, int width, int height) {
        Rectangle newBounds = getNewBoundsIfNotAlmostEquals(this, x, y, width, height);
        super.setBounds(newBounds.x, newBounds.y, newBounds.width,  newBounds.height);
    }
}