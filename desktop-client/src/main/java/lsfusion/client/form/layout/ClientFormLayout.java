package lsfusion.client.form.layout;

import lsfusion.client.ClientActionProxy;
import lsfusion.client.ContainerFocusListener;
import lsfusion.client.FormFocusTraversalPolicy;
import lsfusion.client.MultiAction;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.logics.ClientContainer;
import lsfusion.client.logics.ClientGroupObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientFormLayout extends JPanel {

    public interface KeyBinding {
        public boolean keyPressed(KeyEvent ke);
    }

    private final ClientFormController form;
    private final ClientContainer mainContainer;

    private FormFocusTraversalPolicy policy;

    private Map<KeyStroke, Map<ClientGroupObject, List<KeyBinding>>> bindings = new HashMap<KeyStroke, Map<ClientGroupObject, List<KeyBinding>>>();
    private Map<ClientContainer, ClientContainerView> containerViews = new HashMap<ClientContainer, ClientContainerView>();

    @SuppressWarnings({"FieldCanBeLocal"})
    private FocusListener focusListener;

    public ClientFormLayout(ClientFormController iform, ClientContainer imainContainer) {
        this.form = iform;
        this.mainContainer = imainContainer;

        policy = new FormFocusTraversalPolicy();

        setFocusCycleRoot(true);
        setFocusTraversalPolicy(policy);

        // создаем все контейнеры на форме
        createContainerViews(mainContainer);

        setLayout(new BorderLayout());
        add(containerViews.get(mainContainer).getView(), BorderLayout.CENTER);

        //todo: think about scrollpane, when window size is too small
//        JScrollPane scroll = new JScrollPane(mainContainer);
//        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//        add(scroll, BorderLayout.CENTER);

        // приходится делать StrongRef, иначе он тут же соберется сборщиком мусора так как ContainerFocusListener держит его как WeakReference
        focusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                form.gainedFocus();
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

    // метод рекурсивно создает для каждого ClientContainer соответствующий ContainerView
    private void createContainerViews(ClientContainer container) {
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
            throw new IllegalStateException("Scroll isn't implemented yet");
        } else if (container.isFlow()) {
            throw new IllegalStateException("Flow isn't implemented yet");
        } else {
            throw new IllegalStateException("Illegal container type");
        }

        containerViews.put(container, containerView);

        if (container.container != null) {
            add(container, containerView.getView());
        }

        for (ClientComponent child : container.children) {
            if (child instanceof ClientContainer) {
                createContainerViews((ClientContainer) child);
            }
        }
    }

    public void preValidateMainContainer() {
        autoShowHideContainers(mainContainer);
    }

    private void autoShowHideContainers(ClientContainer container) {
        ClientContainerView containerView = containerViews.get(container);
        if (!containerView.getView().isValid()) {
            int childCnt = containerView.getChildrenCount();
            boolean hasVisible = false;
            for (int i = 0; i < childCnt; ++i) {
                ClientComponent child = containerView.getChild(i);
                Component childView = containerView.getChildView(i);
                if (child instanceof ClientContainer) {
                    autoShowHideContainers((ClientContainer) child);
                }

                if (childView.isVisible()) {
                    hasVisible = true;
                }
            }
            containerView.getView().setVisible(hasVisible);
            containerView.updateLayout();
        }
    }

    // добавляем визуальный компонент
    public boolean add(ClientComponent key, Component view) {
        if (key != null) {
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
        if (key != null) {
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

    public void addBinding(KeyStroke key, String id, AbstractAction action) {
        Object oldId = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(key);

        String resultId = id;
        Action resultAction = new ClientActionProxy(form, action);
        if (oldId != null) {
            Action oldAction = getActionMap().get(oldId);
            if (oldAction != null) {
                MultiAction multiAction = new MultiAction(oldAction);
                multiAction.addAction(resultAction);
                resultId += " and " + oldId;
                resultAction = multiAction;
            }
        }

        getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, resultId);
        getActionMap().put(resultId, resultAction);
    }

    public void addKeyBinding(KeyStroke ks, ClientGroupObject groupObject, KeyBinding binding) {
        Map<ClientGroupObject, List<KeyBinding>> groupBindings = bindings.get(ks);
        if (groupBindings == null) {
            groupBindings = new HashMap<ClientGroupObject, List<KeyBinding>>();
            bindings.put(ks, groupBindings);
        }

        List<KeyBinding> bindings = groupBindings.get(groupObject);
        if (bindings == null) {
            bindings = new ArrayList<KeyBinding>();
            groupBindings.put(groupObject, bindings);
        }

        bindings.add(binding);
    }

    @Override
    public Dimension getMinimumSize() {
        //для таблиц с большим числом колонок возвращается огромное число и тогда Docking Frames пытается всё отдать под форму
        return new Dimension(0, 0);
    }

    // реализуем "обратную" обработку нажатий кнопок
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        Map<ClientGroupObject, List<KeyBinding>> keyBinding = bindings.get(ks);
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT && keyBinding != null && !keyBinding.isEmpty()) {
            // делаем так, чтобы первым нажатия клавиш обрабатывал GroupObject, у которого стоит фокус
            // хотя конечно идиотизм это делать таким образом

            Component comp = ke.getComponent();
            while (comp != null && !(comp instanceof Window) && comp != this) {
                if (comp instanceof JComponent) {
                    ClientGroupObject groupObject = (ClientGroupObject) ((JComponent) comp).getClientProperty("groupObject");
                    if (groupObject != null) {
                        List<KeyBinding> groupBindings = keyBinding.get(groupObject);
                        if (groupBindings != null) {
                            for (KeyBinding binding : groupBindings) {
                                if (binding.keyPressed(ke)) {
                                    return true;
                                }
                            }
                        }
                        break;
                    }
                }
                comp = comp.getParent();
            }

            for (List<KeyBinding> groupBindings : keyBinding.values()) {
                for (KeyBinding binding : groupBindings) {
                    if (binding.keyPressed(ke)) {
                        return true;
                    }
                }
            }

            return true;
        }

        return super.processKeyBinding(ks, ke, condition, pressed);
    }
}
