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

    private transient Map<PropertyViewNavigator, PropertyCellView> mproperties = new HashMap<PropertyViewNavigator, PropertyCellView>();
    public PropertyCellView get(PropertyViewNavigator property) { return mproperties.get(property); }

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

            ContainerView groupContainer = addContainer(group.get(0).caption); // контейнер всей группы
            groupContainer.constraints.order = navigatorForm.groups.indexOf(group);
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
            mainContainer.add(groupContainer);

            groupContainers.put(clientGroup, groupContainer);

            ContainerView gridContainer = addContainer(); // контейнер грида внутрь
            gridContainer.constraints.order = 0;
            gridContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
            groupContainer.add(gridContainer);

            ContainerView panelContainer = addContainer(); // контейнер панели
            panelContainer.constraints.order = 1;
            groupContainer.add(panelContainer);

            panelContainers.put(clientGroup, panelContainer);

            ContainerView controlsContainer = addContainer(); // контейнер всех управляющих объектов
            controlsContainer.constraints.order = 2;
            controlsContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
            controlsContainer.constraints.insetsInside = new Insets(0,0,0,0);
            controlsContainer.constraints.insetsSibling = new Insets(0,0,0,0);
            groupContainer.add(controlsContainer);

            clientGroup.gridView.constraints.order = 1;
            clientGroup.gridView.constraints.fillVertical = 1;
            clientGroup.gridView.constraints.fillHorizontal = 1;
            gridContainer.add(clientGroup.gridView);

            for (ObjectImplementView clientObject : clientGroup) {

                clientObject.objectCellView.constraints.order = -1000 + clientGroup.indexOf(clientObject);
                addComponent(clientGroup, clientObject.objectCellView, clientObject.view.baseClass.getParent());

                order.add(clientObject.objectCellView);

                clientObject.classCellView.constraints.order = -500 + clientGroup.indexOf(clientObject);
                addComponent(clientGroup, clientObject.classCellView, clientObject.view.baseClass.getParent());

                order.add(clientObject.classCellView);

                clientObject.classView.constraints.order = 0;
                clientObject.classView.constraints.fillVertical = 1;
                clientObject.classView.constraints.fillHorizontal = 0.2;
                gridContainer.add(clientObject.classView);

                ContainerView buttonContainer = addContainer(); // контейнер кнопок
                buttonContainer.constraints.order = clientGroup.indexOf(clientObject);
                buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
                controlsContainer.add(buttonContainer);

                buttonContainers.put(clientObject, buttonContainer);

                clientObject.addView.constraints.order = 0;
                buttonContainer.add(clientObject.addView);

                clientObject.delView.constraints.order = 1;
                buttonContainer.add(clientObject.delView);

                clientObject.changeClassView.constraints.order = 2;
                buttonContainer.add(clientObject.changeClassView);

                mobjects.put(clientObject.view, clientObject);
            }

            clientGroup.showTypeView.constraints.order = clientGroup.size();
            clientGroup.showTypeView.constraints.directions = new SimplexComponentDirections(0.01,0,0,0.01);
            controlsContainer.add(clientGroup.showTypeView);
//            clientGroup.showTypeView.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
        }

        for (PropertyViewNavigator control : navigatorForm.propertyViews) {

            GroupObjectImplementView groupObject = mgroupObjects.get(control.toDraw);

            PropertyCellView clientProperty = new PropertyCellView(idGenerator.idShift(), control);
            clientProperty.constraints.order = navigatorForm.propertyViews.indexOf(control);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            mproperties.put(control, clientProperty);
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
            filterGroupView.constraints.order = 3 + navigatorForm.regularFilterGroups.indexOf(filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);
            buttonContainers.get(mgroupObjects.get(navigatorForm.getApplyObject(groupObjects)).get(0)).add(filterGroupView);

            regularFilters.add(filterGroupView);
        }

        ContainerView formButtonContainer = addContainer();
        formButtonContainer.constraints.order = 1000; // начинаем с очень большого числа, поскольку в mainContainer могут попадать и свойства, если toDraw == null
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        mainContainer.add(formButtonContainer);

        printView.constraints.order = 0;
        printView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(printView);

        xlsView.constraints.order = 1;
        xlsView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(xlsView);

        refreshView.constraints.order = 2;
        refreshView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(refreshView);

        applyView.constraints.order = 3;
        applyView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(applyView);

        applyView.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        cancelView.constraints.order = 4;
        cancelView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(cancelView);

        okView.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        okView.constraints.order = 5;
        okView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(okView);

        closeView.constraints.order = 6;
        closeView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(closeView);

    }

    private void addComponent(GroupObjectImplementView groupObject, ComponentView childComponent, AbstractGroup groupAbstract) {

        int order = childComponent.constraints.order;

        while (groupAbstract != null) {

            while (groupAbstract != null && !groupAbstract.createContainer) {
                groupAbstract = groupAbstract.getParent();
            } // пропускаем группы, по которым не нужно создавать контейнер

            if (groupAbstract == null) break;

            if (!groupPropertyContainers.containsKey(groupObject))
                groupPropertyContainers.put(groupObject, new HashMap<AbstractGroup, ContainerView>());

            ContainerView groupPropertyContainer;
            if (groupPropertyContainers.get(groupObject).containsKey(groupAbstract))
                groupPropertyContainer = groupPropertyContainers.get(groupObject).get(groupAbstract);
            else {
                groupPropertyContainer = addContainer(groupAbstract.caption);
                groupPropertyContainer.constraints.order = order;
                groupPropertyContainers.get(groupObject).put(groupAbstract, groupPropertyContainer);
            }

            childComponent.container = groupPropertyContainer;
            childComponent = groupPropertyContainer;

            groupAbstract = groupAbstract.getParent();
        }

        // проверка на null нужна для глобальных свойств без groupObject'ов вообще
        ContainerView groupContainer = panelContainers.get(groupObject);
        childComponent.container = (groupContainer == null) ? mainContainer : groupContainer;  

    }

}
