package platform.client.form;

import platform.client.FormFocusTraversalPolicy;
import platform.client.logics.ClientComponentView;
import platform.client.logics.ClientContainerView;
import platform.client.logics.ClientGroupObjectImplementView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ClientFormLayout extends JPanel {

    // главный контейнер, который будет использоваться при отрисовке формы
    private ClientFormContainer mainContainer;

    private FormFocusTraversalPolicy policy;

    public JComponent getComponent() {
        return mainContainer;
    }

    // объект, которому делегируется ответственность за расположение объектов на форме
    private SimplexLayout layoutManager;

    // отображение объектов от сервера на контейнеры для рисования
    private Map<ClientContainerView, ClientFormContainer> contviews;

    private boolean hasFocus = false;

    protected abstract void gainedFocus();

    public ClientFormLayout(List<ClientContainerView> containers) {

        createContainerViews(containers);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(mainContainer);

        // следим за тем, когда форма становится активной
        final String FOCUS_OWNER_PROPERTY = "focusOwner";

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(FOCUS_OWNER_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                Component focusComponent = (Component)evt.getNewValue();
                if (focusComponent != null) {
                    boolean newHasFocus = (ClientFormLayout.this.isAncestorOf(focusComponent)) | (focusComponent.equals(ClientFormLayout.this));
                    if (hasFocus != newHasFocus) {
                        hasFocus = newHasFocus;
                        if (hasFocus) {
                            gainedFocus();
                        }
                    }
                }

            }
        });

        setFocusCycleRoot(true);
        policy = new FormFocusTraversalPolicy();
        setFocusTraversalPolicy(policy);

        // вот таким вот маразматичным способом делается, чтобы при нажатии мышкой в ClientForm фокус оставался на ней, а не уходил куда-то еще
        // теоретически можно найти способ как это сделать не так извращенно, но копаться в исходниках Swing'а очень долго
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    private void createContainerViews(List<ClientContainerView> containers) {

        contviews = new HashMap<ClientContainerView, ClientFormContainer>();

        // считываем все контейнеры от сервера и создаем контейнеры отображения с соответствующей древовидной структурой
        while (true) {

            boolean found = false;
            for (ClientContainerView container : containers) {
                if ((container.container == null || contviews.containsKey(container.container)) && !contviews.containsKey(container)) {

                    ClientFormContainer contview = new ClientFormContainer(container);
                    contview.setLayout(layoutManager);
                    if (container.container == null) {

                        mainContainer = contview;

                        layoutManager = new SimplexLayout(mainContainer, container.constraints);
                        mainContainer.setLayout(layoutManager);
                    }
                    else {
                        contviews.get(container.container).add(contview, container.constraints);
                    }
                    contviews.put(container, contview);
                    found = true;
                }
            }

            if (!found) break;

        }

    }

    private Map<KeyStroke, Map<ClientGroupObjectImplementView, Runnable>> bindings = new HashMap<KeyStroke, Map<ClientGroupObjectImplementView, Runnable>>();

    public void addKeyBinding(KeyStroke ks, ClientGroupObjectImplementView groupObject, Runnable run) {
        if (!bindings.containsKey(ks))
            bindings.put(ks, new HashMap<ClientGroupObjectImplementView, Runnable>());
        bindings.get(ks).put(groupObject, run);
    }

    // реализуем "обратную" обработку нажатий кнопок
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        // делаем так, чтобы первым нажатия клавиш обрабатывал GroupObject, у которого стоит фокус
        // хотя конечно идиотизм это делать таким образом
        Component comp = e.getComponent(); //KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        while (comp != null && !(comp instanceof Window) && comp != this) {
            if (comp instanceof JComponent) {
                ClientGroupObjectImplementView groupObject = (ClientGroupObjectImplementView)((JComponent)comp).getClientProperty("groupObject");
                if (groupObject != null) {
                    Map<ClientGroupObjectImplementView, Runnable> keyBinding = bindings.get(ks);
                    if (keyBinding != null && keyBinding.containsKey(groupObject)) {
                        keyBinding.get(groupObject).run();
                        return true;
                    }
                    break;
                }
            }
            comp = comp.getParent();
        }

        Map<ClientGroupObjectImplementView, Runnable> keyBinding = bindings.get(ks);
        if (keyBinding != null && !keyBinding.isEmpty())
            keyBinding.values().iterator().next().run();

        if (super.processKeyBinding(ks, e, condition, pressed)) return true;

        return false;
    }

    // добавляем визуальный компонент
    public boolean add(ClientComponentView component, Component view) {
        if (!contviews.get(component.container).isAncestorOf(view)) {
            contviews.get(component.container).addComponent(view, component.constraints);
            contviews.get(component.container).repaint();
            if (component.defaultComponent){
                policy.addDefault(view);
            }
            return true;
        } else
            return false;
    }

    // удаляем визуальный компонент
    public boolean remove(ClientComponentView component, Component view) {
       if (contviews.get(component.container).isAncestorOf(view)) {
            contviews.get(component.container).removeComponent(view);
            contviews.get(component.container).repaint();
            if (component.defaultComponent){
                policy.removeDefault(view);
            }
            return true;
       } else
            return false;
    }

    public void dropCaches() {
        layoutManager.dropCaches();
    }

    public void addBinding(KeyStroke key, String id, AbstractAction action) {
        getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, id);
        getActionMap().put(id, action);
    }
}
