package platform.server.view.form.client;

import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.layout.SingleSimplexConstraint;
import platform.server.logics.properties.groups.AbstractGroup;
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

    private transient Map<GroupObjectImplementView, ContainerView> groupObjectContainers = new HashMap<GroupObjectImplementView, ContainerView>();
    private transient Map<GroupObjectImplementView, ContainerView> panelContainers = new HashMap<GroupObjectImplementView, ContainerView>();
    private transient Map<GroupObjectImplementView, ContainerView> buttonContainers = new HashMap<GroupObjectImplementView, ContainerView>();
    private transient Map<GroupObjectImplementView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectImplementView, Map<AbstractGroup, ContainerView>>();

    public DefaultFormView(NavigatorForm<?> navigatorForm) {

        ContainerView mainContainer = addContainer();
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        for (GroupObjectNavigator group : navigatorForm.groups) {

            GroupObjectImplementView clientGroup = new GroupObjectImplementView(group);

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            ContainerView groupContainer = addContainer();
            groupContainer.container = mainContainer;
            groupContainer.constraints.order = navigatorForm.groups.indexOf(group);
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
            groupContainer.constraints.insetsInside = new Insets(0,0,4,0);

            groupObjectContainers.put(clientGroup, groupContainer);

            ContainerView gridContainer = addContainer();
            gridContainer.container = groupContainer;
            gridContainer.constraints.order = 0;
            gridContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            ContainerView panelContainer = addContainer();
            panelContainer.container = groupContainer;
            panelContainer.constraints.order = 1;
            panelContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;

            panelContainer.title = group.get(0).caption;

            panelContainers.put(clientGroup, panelContainer);

            ContainerView buttonContainer = addContainer();
            buttonContainer.container = groupContainer;
            buttonContainer.constraints.order = 2;
            buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            buttonContainers.put(clientGroup, buttonContainer);

            clientGroup.gridView.container = gridContainer;
            clientGroup.gridView.constraints.order = 1;
            clientGroup.gridView.constraints.fillVertical = 1;
            clientGroup.gridView.constraints.fillHorizontal = 1;

            clientGroup.addView.container = buttonContainer;
            clientGroup.addView.constraints.order = 0;

            clientGroup.delView.container = buttonContainer;
            clientGroup.delView.constraints.order = 1;

            clientGroup.changeClassView.container = buttonContainer;
            clientGroup.changeClassView.constraints.order = 2;

            for (ObjectImplementView clientObject : clientGroup) {

                clientObject.container = panelContainer;
                clientObject.constraints.order = -1000 + clientGroup.indexOf(clientObject);
                clientObject.constraints.insetsSibling = new Insets(0,0,2,2);

                addComponent(clientGroup, clientObject, clientObject.view.baseClass.getParent());

                clientObject.classView.container = gridContainer;
                clientObject.classView.constraints.order = 0;
                clientObject.classView.constraints.fillVertical = 1;
                clientObject.classView.constraints.fillHorizontal = 0.2;

                mobjects.put(clientObject.view, clientObject);

                order.add(clientObject);
            }
        }

        for (PropertyViewNavigator property : navigatorForm.propertyViews) {

            GroupObjectImplementView groupObject = mgroupObjects.get(property.toDraw);

            PropertyCellView clientProperty = new PropertyCellView(property);
            clientProperty.constraints.order = navigatorForm.propertyViews.indexOf(property);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            properties.add(clientProperty);

            addComponent(groupObject, clientProperty, property.view.property.getParent());
            order.add(clientProperty);
        }

        for (RegularFilterGroupNavigator filterGroup : navigatorForm.regularFilterGroups) {

            Set<ObjectNavigator> groupObjects = new HashSet<ObjectNavigator>();

            // ищем самый нижний GroupObjectImplement, к которому применяется фильтр
            for (RegularFilterNavigator regFilter : filterGroup.filters)
                groupObjects.addAll(navigatorForm.getApplyObject(regFilter.filter.getObjects()));

            RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup);
            filterGroupView.container = buttonContainers.get(mgroupObjects.get(navigatorForm.getApplyObject(groupObjects)));
            filterGroupView.constraints.order = 3 + navigatorForm.regularFilterGroups.indexOf(filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);

            regularFilters.add(filterGroupView);
        }

        ContainerView formButtonContainer = addContainer();
        formButtonContainer.container = mainContainer;
        formButtonContainer.constraints.order = navigatorForm.groups.size();
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        printView.container = formButtonContainer;
        printView.constraints.order = 0;
        printView.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);

        refreshView.container = formButtonContainer;
        refreshView.constraints.order = 1;
        refreshView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        applyView.container = formButtonContainer;
        applyView.constraints.order = 2;
        applyView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        applyView.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        cancelView.container = formButtonContainer;
        cancelView.constraints.order = 3;
        cancelView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        okView.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        okView.container = formButtonContainer;
        okView.constraints.order = 4;
        okView.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);

        closeView.container = formButtonContainer;
        closeView.constraints.order = 5;
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
            groupAbstract = groupAbstract.getParent();
        }

        childComponent.container = panelContainers.get(groupObject);

    }

}
