package lsfusion.client.form.design.view;

import lsfusion.client.base.focus.ContainerFocusListener;
import lsfusion.client.base.focus.FormFocusTraversalPolicy;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.event.KeyInputEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class ClientFormLayout extends JPanel {

    public Dimension getMaxPreferredSize() {
        return AbstractClientContainerView.getMaxPreferredSize(mainContainer,containerViews, false); // в BOX container'е берем явный size (предполагая что он используется не как базовый размер с flex > 0, а конечный)
    }

    private final ClientFormController form;
    private final ClientContainer mainContainer;

    private FormFocusTraversalPolicy policy;
    
    private Map<ClientContainer, ClientContainerView> containerViews = new HashMap<>();
    
    private boolean blocked;

    @SuppressWarnings({"FieldCanBeLocal"})
    private FocusListener focusListener;

    public JComponentPanel getComponentView(ClientContainer container) {
        return getContainerView(container).getView();
    }

    public ClientFormLayout(ClientFormController iform, ClientContainer imainContainer) {
        this.form = iform;
        this.mainContainer = imainContainer;

        policy = new FormFocusTraversalPolicy();

        setFocusCycleRoot(true);
        setFocusTraversalPolicy(policy);

        setLayout(new BorderLayout());

        addContainers(mainContainer);
        
        JScrollPane scroll = new JScrollPane() {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(null); // is set on every color theme change in installDefaults()
            }
        };
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getHorizontalScrollBar().setUnitIncrement(14);
        scroll.setViewportView(getComponentView(mainContainer));
        // to forward a mouse wheel event in nested scroll pane to the parent scroll pane
        JLayer<JScrollPane> scrollLayer = new JLayer<>(scroll, new MouseWheelScrollLayerUI());
        add(scrollLayer, BorderLayout.CENTER);

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
    }

    public ClientContainerView getContainerView(ClientContainer container) {
        return containerViews.get(container);
    }

    // метод рекурсивно создает для каждого ClientContainer соответствующий ContainerView
    private void addContainers(ClientContainer container) {
        ClientContainerView containerView;
        if (container.isLinear()) {
            containerView = new LinearClientContainerView(this, container);
        } else if (container.isSplit()) {
            containerView = new SplitClientContainerView(this, container);
        } else if (container.isTabbed()) {
            containerView = new TabbedClientContainerView(this, container, form);
        } else if (container.isColumns()) {
            containerView = new ColumnsClientContainerView(this, container);
        } else if (container.isScroll()) {
            containerView = new ScrollClientContainerView(this, container);
        } else if (container.isFlow()) {
            throw new IllegalStateException("Flow isn't implemented yet");
        } else {
            throw new IllegalStateException("Illegal container type");
        }

        containerViews.put(container, containerView);

        add(container, containerView.getView());

        for (ClientComponent child : container.children) {
            if (child instanceof ClientContainer) {
                addContainers((ClientContainer) child);
            }
        }
    }

    // вообще раньше была в validate, calculatePreferredSize видимо для устранения каких-то визуальных эффектов
    // но для activeTab нужно вызвать предварительно, так как вкладка может только-только появится
    // пока убрал (чтобы было как в вебе), но если будут какие-то нежелательные эффекты, можно будет вернуть а в activeElements поставить только по условию, что есть activeTabs или activeProps
    public void preValidateMainContainer() { // hideEmptyContainerViews 
        autoShowHideContainers(mainContainer);
    }

    private void autoShowHideContainers(ClientContainer container) {
        ClientContainerView containerView = containerViews.get(container);
//        if (!containerView.getView().isValid()) { // непонятная проверка, valid достаточно непредсказуемая штука и логически не сильно связано с логикой visibility container'ов + вызывается огранич
            int childCnt = containerView.getChildrenCount();
            boolean hasVisible = false;
            for (int i = 0; i < childCnt; ++i) {
                ClientComponent child = containerView.getChild(i);
                Component childView = containerView.getChildView(i);
                if (child instanceof ClientContainer) {
                    autoShowHideContainers((ClientContainer) child);
                }

                //difference between desktop and web: ClientFilter is not dialog box, it not extend ClientContainer and is in children list
                if (childView.isVisible() && !(child instanceof ClientFilter)) {
                    hasVisible = true;
                }
            }
            containerView.getView().setVisible(hasVisible);
            containerView.updateLayout();
//        }
    }

    // добавляем визуальный компонент
    public boolean add(ClientComponent key, JComponentPanel view) {
        if (key.container != null) { // container can be null when component should be layouted manually
            ClientContainerView containerView = containerViews.get(key.container);
            if (containerView != null && !containerView.hasChild(key)) {
                revalidate();
                repaint();

                containerView.add(key, view);

                if (key.defaultComponent) {
                    policy.addDefault(view);
                }
                return true;
            }
        }
        return false;
    }

    // удаляем визуальный компонент
    public boolean remove(ClientComponent key, Component view) {
        if (key.container != null) { // see add method
            ClientContainerView containerView = containerViews.get(key.container);
            if (containerView != null && containerView.hasChild(key)) {
                revalidate();
                repaint();

                containerView.remove(key);
                if (key.defaultComponent) {
                    policy.removeDefault(view);
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

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            if(pressed && form.processBinding(new KeyInputEvent(ks), ke, () -> getGroupObject(ke.getComponent()), true))
                return true;
        }

        return super.processKeyBinding(ks, ke, condition, pressed);
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
