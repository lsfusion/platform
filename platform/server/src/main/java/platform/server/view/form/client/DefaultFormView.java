package platform.server.view.form.client;

import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.layout.SingleSimplexConstraint;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.view.navigator.*;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultFormView extends FormView {

    private transient Map<GroupObjectNavigator, GroupObjectImplementView> mgroupObjects = new HashMap<GroupObjectNavigator, GroupObjectImplementView>();
    public GroupObjectImplementView get(GroupObjectNavigator groupObject) { return mgroupObjects.get(groupObject); }

    private transient Map<ObjectNavigator, ObjectImplementView> mobjects = new HashMap<ObjectNavigator, ObjectImplementView>();
    public ObjectImplementView get(ObjectNavigator object) { return mobjects.get(object); }

    private transient ContainerView mainContainer;
    public ContainerView getMainContainer() { return mainContainer; }

    private transient Map<GroupObjectImplementView, ContainerView> panelContainers = new HashMap<GroupObjectImplementView, ContainerView>();
    public ContainerView getPanelContainer(GroupObjectImplementView groupObject) { return panelContainers.get(groupObject); }

    private transient Map<GroupObjectImplementView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectImplementView, Map<AbstractGroup, ContainerView>>();

    private transient Map<GroupObjectImplementView, ContainerView> groupContainers = new HashMap<GroupObjectImplementView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectImplementView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectNavigator groupObject) { return getGroupObjectContainer(get(groupObject)); }

    public DefaultFormView(NavigatorForm<?> navigatorForm) {

        mainContainer = addContainer();
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        Map<ObjectImplementView, ContainerView> buttonContainers = new HashMap<ObjectImplementView, ContainerView>();
        for (GroupObjectNavigator group : navigatorForm.groups) {

            GroupObjectImplementView clientGroup = new GroupObjectImplementView(idGenerator, group);

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            ContainerView groupContainer = addContainer(); // контейнер всей группы
            groupContainer.title = group.get(0).caption;
            groupContainer.container = mainContainer;
            groupContainer.constraints.order = navigatorForm.groups.indexOf(group);
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

            groupContainers.put(clientGroup, groupContainer);

            ContainerView gridContainer = addContainer(); // контейнер грида внутрь
            gridContainer.container = groupContainer;
            gridContainer.constraints.order = 0;
            gridContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            ContainerView panelContainer = addContainer(); // контейнер панели
            panelContainer.container = groupContainer;
            panelContainer.constraints.order = 1;
            panelContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;

            panelContainers.put(clientGroup, panelContainer);

            ContainerView controlsContainer = addContainer(); // контейнер всех управляющих объектов
            controlsContainer.container = groupContainer;
            controlsContainer.constraints.order = 2;
            controlsContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
            controlsContainer.constraints.insetsInside = new Insets(0,0,0,0);
            controlsContainer.constraints.insetsSibling = new Insets(0,0,0,0);

            clientGroup.gridView.container = gridContainer;
            clientGroup.gridView.constraints.order = 1;
            clientGroup.gridView.constraints.fillVertical = 1;
            clientGroup.gridView.constraints.fillHorizontal = 1;

            for (ObjectImplementView clientObject : clientGroup) {

                clientObject.objectCellView.container = panelContainer;
                clientObject.objectCellView.constraints.order = -1000 + clientGroup.indexOf(clientObject);

                addComponent(clientGroup, clientObject.objectCellView, clientObject.view.baseClass.getParent());

                order.add(clientObject.objectCellView);

                clientObject.classCellView.container = panelContainer;
                clientObject.classCellView.constraints.order = -500 + clientGroup.indexOf(clientObject);

                addComponent(clientGroup, clientObject.classCellView, clientObject.view.baseClass.getParent());

                order.add(clientObject.classCellView);

                clientObject.classView.container = gridContainer;
                clientObject.classView.constraints.order = 0;
                clientObject.classView.constraints.fillVertical = 1;
                clientObject.classView.constraints.fillHorizontal = 0.2;

                ContainerView buttonContainer = addContainer(); // контейнер кнопок
                buttonContainer.container = controlsContainer;
                buttonContainer.constraints.order = clientGroup.indexOf(clientObject);
                buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

                buttonContainers.put(clientObject, buttonContainer);

                clientObject.addView.container = buttonContainer;
                clientObject.addView.constraints.order = 0;

                clientObject.delView.container = buttonContainer;
                clientObject.delView.constraints.order = 1;

                clientObject.changeClassView.container = buttonContainer;
                clientObject.changeClassView.constraints.order = 2;

                mobjects.put(clientObject.view, clientObject);
            }

            clientGroup.showTypeView.container = controlsContainer;
            clientGroup.showTypeView.constraints.order = clientGroup.size();
//            clientGroup.showTypeView.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
        }

        for (PropertyViewNavigator control : navigatorForm.propertyViews) {

            GroupObjectImplementView groupObject = mgroupObjects.get(control.toDraw);

            PropertyCellView clientProperty = new PropertyCellView(idGenerator.idShift(), control);
            clientProperty.constraints.order = navigatorForm.propertyViews.indexOf(control);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            properties.add(clientProperty);

            addComponent(groupObject, clientProperty, control.view.property.getParent());
            order.add(clientProperty);
        }

        for (RegularFilterGroupNavigator filterGroup : navigatorForm.regularFilterGroups) {

            Set<ObjectNavigator> groupObjects = new HashSet<ObjectNavigator>();

            // ищем самый нижний GroupObjectImplement, к которому применяется фильтр
            for (RegularFilterNavigator regFilter : filterGroup.filters)
                groupObjects.addAll(navigatorForm.getApplyObject(regFilter.filter.getObjects()));

            RegularFilterGroupView filterGroupView = new RegularFilterGroupView(idGenerator.idShift(), filterGroup);
            filterGroupView.container = buttonContainers.get(mgroupObjects.get(navigatorForm.getApplyObject(groupObjects)).get(0));
            filterGroupView.constraints.order = 3 + navigatorForm.regularFilterGroups.indexOf(filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);

            regularFilters.add(filterGroupView);
        }

        ContainerView formButtonContainer = addContainer();
        formButtonContainer.container = mainContainer;
        formButtonContainer.constraints.order = 1000; // начинаем с очень большого числа, поскольку в mainContainer могут попадать и свойства, если toDraw == null
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        printView.container = formButtonContainer;
        printView.constraints.order = 0;
        printView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);

        xlsView.container = formButtonContainer;
        xlsView.constraints.order = 1;
        xlsView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);

        refreshView.container = formButtonContainer;
        refreshView.constraints.order = 2;
        refreshView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        applyView.container = formButtonContainer;
        applyView.constraints.order = 3;
        applyView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        applyView.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        cancelView.container = formButtonContainer;
        cancelView.constraints.order = 4;
        cancelView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        okView.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        okView.container = formButtonContainer;
        okView.constraints.order = 5;
        okView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        closeView.container = formButtonContainer;
        closeView.constraints.order = 6;
        closeView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

    }

    private void addComponent(GroupObjectImplementView groupObject, ComponentView childComponent, AbstractGroup groupAbstract) {

        int order = childComponent.constraints.order;

        while (groupAbstract != null) {

            if (!groupPropertyContainers.containsKey(groupObject))
                groupPropertyContainers.put(groupObject, new HashMap<AbstractGroup, ContainerView>());

            ContainerView groupPropertyContainer;
            if (groupPropertyContainers.get(groupObject).containsKey(groupAbstract))
                groupPropertyContainer = groupPropertyContainers.get(groupObject).get(groupAbstract);
            else {
                groupPropertyContainer = addContainer();
                groupPropertyContainers.get(groupObject).put(groupAbstract, groupPropertyContainer);
                groupPropertyContainer.constraints.order = order;
                groupPropertyContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
                groupPropertyContainer.constraints.fillVertical = -1;
                groupPropertyContainer.constraints.fillHorizontal = -1;
                groupPropertyContainer.title = groupAbstract.caption;
            }

            childComponent.container = groupPropertyContainer;
            childComponent = groupPropertyContainer;
            do {
                groupAbstract = groupAbstract.getParent();
            } while (groupAbstract != null && !groupAbstract.container); // пропускаем группы, по которым не нужно создавать контейнер

        }

        // проверка на null нужна для глобальных свойств без groupObject'ов вообще
        ContainerView groupContainer = panelContainers.get(groupObject);
        childComponent.container = (groupContainer == null) ? mainContainer : groupContainer;  

    }

}
