package platform.client.form;

import platform.interop.form.layout.SimplexLayout;
import platform.client.logics.ClientContainerView;
import platform.client.logics.ClientComponentView;

import javax.swing.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.awt.*;

class ClientFormLayout {

    // главный контейнер, который будет использоваться при отрисовке формы
    private ClientFormContainer mainContainer;

    public JComponent getComponent() {
        return mainContainer;
    }

    // объект, которому делегируется ответственность за расположение объектов на форме
    private SimplexLayout layoutManager;

    // отображение объектов от сервера на контейнеры для рисования
    private Map<ClientContainerView, ClientFormContainer> contviews;

    public ClientFormLayout(List<ClientContainerView> containers) {

        createContainerViews(containers);
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

    // добавляем визуальный компонент
    public boolean add(ClientComponentView component, Component view) {
        if (!contviews.get(component.container).isAncestorOf(view)) {
            contviews.get(component.container).addComponent(view, component.constraints);
            contviews.get(component.container).repaint();
            return true;
        } else
            return false;
    }

    // удаляем визуальный компонент
    public boolean remove(ClientComponentView component, Component view) {
       if (contviews.get(component.container).isAncestorOf(view)) {
            contviews.get(component.container).removeComponent(view);
            contviews.get(component.container).repaint();
            return true;
       } else
            return false;
    }

}
