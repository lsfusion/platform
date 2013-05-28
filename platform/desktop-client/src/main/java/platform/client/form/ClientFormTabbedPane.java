package platform.client.form;

import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.interop.form.layout.InsetTabbedPane;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static platform.client.ClientResourceBundle.getString;
import static platform.client.SwingUtils.getNewBoundsIfNotAlmostEquals;

public class ClientFormTabbedPane extends JTabbedPane implements AutoHideableContainer, InsetTabbedPane {

    LayoutManager2 layout;

    private int minHeight;

    private Dimension tabInsets;

    private ClientComponent selectedTab;

    public ClientFormTabbedPane(final ClientContainer key, final ClientFormController form, LayoutManager2 layout) {

        this.layout = layout;

        key.design.designComponent(this);

        // таким вот волшебным способом рассчитывается минимальная высота и отступы использующиеся при отрисовке Tab'ов
        // добавляется тестовый компонент, считаются размеры и все удаляются
        // иначе делать очень тяжело
        Container testContainer = new Container();
        addTab("", testContainer);

        Dimension minSize = getMinimumSize();
        Dimension contSize = testContainer.getMinimumSize();
        minHeight = minSize.height;

        tabInsets = new Dimension(4, minSize.height - contSize.height);

        removeAll();

        if(key.children.size() > 0)
            selectedTab = key.children.iterator().next();

        addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int selected = getSelectedIndex();
                int visible = 0;
                for(ClientComponent child : key.children)
                    if(clientComponents.containsKey(child) && indexOfComponent(clientComponents.get(child)) != -1) {
                        if(visible++==selected) {
                            if(child!=selectedTab) { // вообще changeListener может вызваться при инициализации, но это проверка в том числе позволяет suppres'ить этот случай
                                try {
                                    form.setTabVisible(key, child);
                                    selectedTab = child;
                                } catch (IOException ex) {
                                    throw new RuntimeException(getString("errors.error.changing.sorting"), ex);
                                }
                            }
                            return;
                        }
                    }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    transferFocus();
                }
            }
        });
    }

    private Map<Component, Object> addedComponents = new HashMap<Component, Object>();
    private Map<Object, Component> clientComponents = new HashMap<Object, Component>();

    public Set<Component> getAddedComponents() {
        return addedComponents.keySet();
    }

    @Override
    public void add(Component component, Object constraints) {
        SimplexLayout.showHideableContainers(this);

        layout.addLayoutComponent(component, constraints);
        addedComponents.put(component, constraints); // важно чтобы до, так как listener'у нужно найти компоненту чтобы послать notification на сервер
        clientComponents.put(constraints, component);
        show(component, constraints);

        adjustMinimumSize();
    }

    @Override
    public void remove(Component component) {
        clientComponents.remove(addedComponents.get(component));
        addedComponents.remove(component);
        super.remove(component);
        layout.removeLayoutComponent(component);
        adjustMinimumSize();
    }

    public void hide(Component component) {
        super.remove(component);
        adjustMinimumSize();
    }

    private void adjustMinimumSize() {
        setMinimumSize(null);
        Dimension minimumSize = getMinimumSize();
        minimumSize.height = minHeight;
        setMinimumSize(minimumSize);
    }

    public void show(Component comp) {
        show(comp, addedComponents.get(comp));
    }

    public void show(Component comp, Object constraints) {

        // вставляем Tab в то место, в котором он идет в container.children
        if (constraints instanceof ClientComponent && ((ClientComponent)constraints).container != null) {
            ClientComponent clientComp = (ClientComponent)constraints;
            ClientContainer clientCont = clientComp.container;

            int tabCount = getTabCount();
            int index;
            for (index = 0; index < tabCount; index++) {
                Component tabComp = getComponentAt(index);
                if (addedComponents.get(tabComp) instanceof ClientComponent) {
                    ClientComponent curComp = (ClientComponent)addedComponents.get(tabComp);
                    if (clientCont.equals(curComp.container) && clientCont.children.indexOf(curComp) > clientCont.children.indexOf(clientComp))
                        break;
                }
            }

            insertTab(clientComp.getCaption(), null, comp, null, index);
        } else
            add("", comp);
    }

    public Dimension getTabInsets() {
        return tabInsets;
    }

    //Чтобы лэйаут не прыгал игнорируем мелкие изменения координат
    @Override
    public void setBounds(int x, int y, int width, int height) {
        Rectangle newBounds = getNewBoundsIfNotAlmostEquals(this, x, y, width, height);
        super.setBounds(newBounds.x, newBounds.y, newBounds.width,  newBounds.height);
    }
}
