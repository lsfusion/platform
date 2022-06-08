package lsfusion.client.form.design.view;

import lsfusion.client.base.focus.ContainerFocusListener;
import lsfusion.client.base.focus.FormFocusTraversalPolicy;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.flex.LinearClientContainerView;
import lsfusion.client.form.design.view.widget.PanelWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.table.grid.view.GridView;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class ClientFormLayout extends PanelWidget {

    public Dimension getMaxPreferredSize(int extraHorzOffset, int extraVertOffset) {
        Integer width = mainContainer.getSize(false);
        Integer height = mainContainer.getSize(true);

        Widget main = getMainView();
        try {
            GridView.calcMaxPrefSize = true;
            invalidate((Component) main);
            Dimension maxPrefSize = main.getPreferredSize();
            return new Dimension(width != null ? width : maxPrefSize.width + extraHorzOffset, height != null ? height : maxPrefSize.height + extraVertOffset);
        } finally {
            GridView.calcMaxPrefSize = false;
            invalidate((Component) main);
        }
    }

    private void invalidate(Component component) {
        for (Component child : ((Container) component).getComponents()) {
            child.invalidate();
            if (child instanceof Container) {
                invalidate(child);
            }
        }
    }

    private final ClientFormController form;
    private final ClientContainer mainContainer;

    private FormFocusTraversalPolicy policy;
    
    private Map<ClientContainer, ClientContainerView> containerViews = new HashMap<>();
    
    private boolean blocked;

    @SuppressWarnings({"FieldCanBeLocal"})
    private FocusListener focusListener;

    public Widget getComponentView(ClientContainer container) {
        return getContainerView(container).getView();
    }

    public ClientFormLayout(ClientFormController iform, ClientContainer imainContainer) {
        super(new BorderLayout());

        this.form = iform;
        this.mainContainer = imainContainer;

        policy = new FormFocusTraversalPolicy();

        setFocusCycleRoot(true);
        setFocusTraversalPolicy(policy);

        addContainers(mainContainer);

        Widget mainView = getComponentView(mainContainer);
        add(AbstractClientContainerView.wrapOverflowAuto(mainView, true, true).getComponent(), BorderLayout.CENTER);

        // приходится делать StrongRef, иначе он тут же соберется сборщиком мусора так как ContainerFocusListener держит его как WeakReference
        focusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                form.gainedFocus();
                MainFrame.instance.setCurrentForm(form);
            }
        };

        ContainerFocusListener.addListener(this, focusListener);

        // вот таким вот маразматичным способом делается, чтобы при нажатии мышкой в ClientFormController фокус оставался на ней, а не уходил куда-то еще
        // теоретически можно найти способ как это сделать не так извращенно, но копаться в исходниках Swing'а очень долго
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    public Widget getMainView() {
        return getContainerView(mainContainer).getView();
    }

    public void directProcessKeyEvent(KeyEvent e) {
        processKeyEvent(e);
    }

    public ClientContainerView getContainerView(ClientContainer container) {
        return containerViews.get(container);
    }

    // метод рекурсивно создает для каждого ClientContainer соответствующий ContainerView
    private void addContainers(ClientContainer container) {
        ClientContainerView containerView;
        if (container.tabbed) {
            containerView = new TabbedClientContainerView(form, container);
        } else {
            containerView = new LinearClientContainerView(form, container);
        }

        containerViews.put(container, containerView);

        Widget viewWidget = containerView.getView();
        // debug info
        if (container.getSID() != null)
            viewWidget.setDebugContainer(container);

        add(container, viewWidget);

        for (ClientComponent child : container.children) {
            if (child instanceof ClientContainer) {
                addContainers((ClientContainer) child);
            }
        }
    }

    public void updatePanels() {
        FlexPanel.updatePanels(getMainView());
    }

    public void autoShowHideContainers() { // hideEmptyContainerViews
        autoShowHideContainers(mainContainer);

        updatePanels();
    }

    private boolean autoShowHideContainers(ClientContainer container) {
        ClientContainerView containerView = getContainerView(container);
        boolean hasVisible = false;
        int size = containerView.getChildrenCount();
        boolean[] childrenVisible = new boolean[size];
        for (int i = 0; i < size; ++i) {
            ClientComponent child = containerView.getChild(i);

            boolean childVisible;
            if (child instanceof ClientContainer)
                childVisible = autoShowHideContainers((ClientContainer) child);
            else {
                Widget childView = baseComponentViews.get(child); // we have to use baseComponentView (and not a wrapper in getChildView), since it has relevant visible state
                childVisible = childView != null && childView.isVisible();
            }

            childrenVisible[i] = childVisible;
            hasVisible = hasVisible || childVisible;
        }
        containerView.updateLayout(childrenVisible);
        return hasVisible;
    }

    private Map<ClientComponent, Widget> baseComponentViews = new HashMap<>();

    public void addBaseComponent(ClientComponent component, Widget view) {
        addBaseComponent(component, view, view.getComponent());
    }

    public void addBaseComponent(ClientComponent component, Widget view, Object focusReceiver) {
        assert !(component instanceof ClientContainer);
        baseComponentViews.put(component, view);
        add(component, view, focusReceiver);
    }

    public void setBaseComponentVisible(ClientComponent component, boolean visible) {
        Widget widget = baseComponentViews.get(component);
        if(widget != null) {
            widget.setVisible(visible);
        }
    }

    // добавляем визуальный компонент
    public boolean add(ClientComponent key, Widget view) {
        return add(key, view, view.getComponent());
    }

    public boolean add(ClientComponent key, Widget view, Object focusReceiver) {
        if (key.container != null) { // container can be null when component should be layouted manually
            ClientContainerView containerView = containerViews.get(key.container);
            if (containerView != null && !containerView.hasChild(key)) {
                revalidate();
                repaint();

                containerView.add(key, view);

                if (key.defaultComponent) {
                    policy.addDefault(focusReceiver);
                }
                return true;
            }
        }
        return false;
    }

    public void removeBaseComponent(ClientComponent key, Widget view) {
        assert !(key instanceof ClientContainer);
        baseComponentViews.remove(key);
        remove(key, view);
    }

    // удаляем визуальный компонент
    public boolean remove(ClientComponent key, Widget view) {
        if (key.container != null) { // see add method
            ClientContainerView containerView = containerViews.get(key.container);
            if (containerView != null && containerView.hasChild(key)) {
                revalidate();
                repaint();

                containerView.remove(key);
                if (key.defaultComponent) {
                    policy.removeDefault(view.getComponent());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Dimension getMinimumSize() {
        //для таблиц с большим числом колонок возвращается огромное число и тогда Docking Frames пытается всё отдать под форму
        return new Dimension(0, 0);
    }

    public ClientGroupObject getGroupObject(Component comp) {
        while (comp != null && !(comp instanceof Window) && comp != this) {
            if (comp instanceof JComponent) {
                ClientGroupObject groupObject = (ClientGroupObject) ((JComponent) comp).getClientProperty("groupObject");
                if (groupObject != null)
                    return groupObject;
            }
            comp = comp.getParent();
        }
        return null;
    }

    private void checkMouseEvent(MouseEvent e, boolean preview) {
        form.checkMouseEvent(e, preview, null, () -> null, false);
    }

    private void checkKeyEvent(KeyStroke ks, boolean preview, KeyEvent e, int condition, boolean pressed) {
        form.checkKeyEvent(ks, e, preview, null, () -> null, false, condition, pressed);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        checkMouseEvent(e, true);

        super.processMouseEvent(e);

        checkMouseEvent(e, false);
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        checkKeyEvent(ks, true, ke, condition, pressed);

        boolean consumed = ke.isConsumed() || super.processKeyBinding(ks, ke, condition, pressed);

        checkKeyEvent(ks, false, ke, condition, pressed);

        return consumed || ke.isConsumed();
    }

    public boolean directProcessKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        return processKeyBinding(ks, ke, condition, pressed);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
