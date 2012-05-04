package platform.client.form;

import platform.client.ClientActionProxy;
import platform.client.ContainerFocusListener;
import platform.client.FormFocusTraversalPolicy;
import platform.client.MultiAction;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientGroupObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public abstract class ClientFormLayout extends JPanel {

    // главный контейнер, который будет использоваться при отрисовке формы
    private JComponent mainContainer;

    private FormFocusTraversalPolicy policy;

    public JComponent getComponent() {
        return mainContainer;
    }

    public Dimension calculatePreferredSize() {
        return layoutManager.calculatePreferredSize();
    }

    // объект, которому делегируется ответственность за расположение объектов на форме
    private SimplexLayout layoutManager;

    // отображение объектов от сервера на контейнеры для рисования
    private Map<ClientContainer, JComponent> contviews = new HashMap<ClientContainer, JComponent>();

    public abstract void gainedFocus();

    @SuppressWarnings({"FieldCanBeLocal"})
    private FocusListener focusListener;

    public ClientFormLayout(ClientContainer topContainer) {

        // создаем все контейнеры на форме
        createContainerViews(topContainer);

        setLayout(new BorderLayout());
        add(mainContainer, BorderLayout.CENTER);
//        JScrollPane scroll = new JScrollPane(mainContainer);
//        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//        add(scroll, BorderLayout.CENTER);

        // приходится делать StrongRef, иначе он тут же соберется сборщиком мусора так как ContainerFocusListener держит его как WeakReference
        focusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                gainedFocus();
            }
        };

        ContainerFocusListener.addListener(this, focusListener);

        setFocusCycleRoot(true);
        policy = new FormFocusTraversalPolicy();
        setFocusTraversalPolicy(policy);

        // вот таким вот маразматичным способом делается, чтобы при нажатии мышкой в ClientFormController фокус оставался на ней, а не уходил куда-то еще
        // теоретически можно найти способ как это сделать не так извращенно, но копаться в исходниках Swing'а очень долго
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    // метод рекурсивно создает для каждого ClientContainer свой ClientFormContainer или ClientFormTabbedPane
    private void createContainerViews(ClientContainer container) {

        JComponent formContainer;
        if (container.isTabbedPane()) {
            formContainer = new ClientFormTabbedPane(container, layoutManager);
        } else if (container.isSplitPane()) {
            formContainer = new ClientFormSplitPane(container, layoutManager, this);
        } else {
            formContainer = new ClientFormContainer(container);
        }

        if (container.container == null) {
            mainContainer = formContainer;
            layoutManager = new SimplexLayout(mainContainer, container);
        } else {
            JComponent parent = contviews.get(container.container);
            parent.add(formContainer, container);
            if (!(parent instanceof JTabbedPane) && formContainer instanceof ClientFormContainer) {
                ((ClientFormContainer) formContainer).addBorder();
            }
        }

        // нельзя перегружать LayoutManager у JTabbedPane, который не наследуется от TabbedPaneLayout
        // поскольку он при расположении заполняют кучу private field'ов, которые в дальнейшем используются при отрисовке JTabbedPane
        // вместо этого SimplexLayout передается в ClientFormTabbedPane и работает как бы "дополнительным" LayoutManager
        if (!container.isTabbedPane() && !container.isSplitPane()) {
            formContainer.setLayout(layoutManager);
        }

        contviews.put(container, formContainer);

        for (ClientComponent child : container.children) {
            if (child instanceof ClientContainer) {
                createContainerViews((ClientContainer) child);
            }
        }
    }

    // добавляем визуальный компонент
    public boolean add(ClientComponent key, Component view) {
        if (key == null) {
            return false;
        }

        JComponent keyContView = contviews.get(key.container);
        if (keyContView == null) {
            return false;
        }

        if (!keyContView.isAncestorOf(view)) {
            keyContView.add(view, key);
            keyContView.repaint();
            if (key.defaultComponent) {
                policy.addDefault(view);
            }
            return true;
        } else {
            return false;
        }
    }

    // удаляем визуальный компонент
    public boolean remove(ClientComponent key, Component view) {
        if (key == null) {
            return false;
        }

        JComponent keyContView = contviews.get(key.container);
        if (keyContView == null) {
            return false;
        }

        if (keyContView.isAncestorOf(view)) {
            keyContView.remove(view);
            keyContView.repaint();
            if (key.defaultComponent) {
                policy.removeDefault(view);
            }
            return true;
        } else {
            return false;
        }
    }

    public void dropCaches() {
        layoutManager.dropCaches();
        mainContainer.revalidate();
    }

    public void addBinding(KeyStroke key, String id, AbstractAction action) {

        Object oldId = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(key);

        String resultId = id;
        Action resultAction = new ClientActionProxy(action);
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

    private Map<KeyStroke, Map<ClientGroupObject, KeyListener>> bindings = new HashMap<KeyStroke, Map<ClientGroupObject, KeyListener>>();

    public void addKeyBinding(KeyStroke ks, ClientGroupObject groupObject, KeyListener run) {
        if (!bindings.containsKey(ks)) {
            bindings.put(ks, new HashMap<ClientGroupObject, KeyListener>());
        }
        bindings.get(ks).put(groupObject, run);
    }

    // реализуем "обратную" обработку нажатий кнопок
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        Map<ClientGroupObject, KeyListener> keyBinding = bindings.get(ks);
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT && keyBinding != null) {
            // делаем так, чтобы первым нажатия клавиш обрабатывал GroupObject, у которого стоит фокус
            // хотя конечно идиотизм это делать таким образом

            Component comp = e.getComponent();
            while (comp != null && !(comp instanceof Window) && comp != this) {
                if (comp instanceof JComponent) {
                    ClientGroupObject groupObject = (ClientGroupObject) ((JComponent) comp).getClientProperty("groupObject");
                    if (groupObject != null) {
                        if (keyBinding != null && keyBinding.containsKey(groupObject)) {
                            keyBinding.get(groupObject).keyPressed(e);
                            return true;
                        }
                        break;
                    }
                }
                comp = comp.getParent();
            }
            if (keyBinding != null && !keyBinding.isEmpty()) {
                keyBinding.values().iterator().next().keyPressed(e);
            }
        }

        return super.processKeyBinding(ks, e, condition, pressed);
    }
}
