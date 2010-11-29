package platform.client.form;

import platform.base.BaseUtils;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.interop.form.layout.InsetTabbedPane;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ClientFormTabbedPane extends JTabbedPane implements AutoHideableContainer, InsetTabbedPane {

    LayoutManager2 layout;

    private int minHeight;

    private Dimension tabInsets;

    public ClientFormTabbedPane(ClientContainer key, LayoutManager2 layout) {

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

        tabInsets = new Dimension(minSize.width - contSize.width, minSize.height - contSize.height);

        removeAll();
    }

    private Map<Component, Object> addedComponents = new HashMap<Component, Object>();

    @Override
    public void add(Component component, Object constraints) {
        SimplexLayout.showHideableContainers(this);

        layout.addLayoutComponent(component, constraints);
        addTab(component, constraints);
        addedComponents.put(component, constraints);

        adjustMinimumSize();
    }

    @Override
    public void remove(Component component) {
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

    public void showAllComponents() {
        for (Map.Entry<Component,Object> comp : addedComponents.entrySet())
            if (indexOfComponent(comp.getKey()) == -1)
                addTab(comp.getKey(), comp.getValue());
    }

    private void addTab(Component comp, Object constraints) {

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
}