package platform.server.view.form;

import platform.server.view.form.*;
import platform.server.view.navigator.NavigatorForm;
import platform.server.logics.properties.groups.AbstractGroup;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.awt.*;

import platform.interop.*;
import platform.client.form.layout.SingleSimplexConstraint;
import platform.client.form.layout.SimplexComponentDirections;

public class DefaultClientFormView extends ClientFormView {

    private transient Map<GroupObjectImplement, ClientGroupObjectImplement> mgroupObjects = new HashMap<GroupObjectImplement, ClientGroupObjectImplement>();
    public ClientGroupObjectImplement get(GroupObjectImplement groupObject) { return mgroupObjects.get(groupObject); }

    private transient Map<ObjectImplement, ClientObjectImplement> mobjects = new HashMap<ObjectImplement, ClientObjectImplement>();
    public ClientObjectImplement get(ObjectImplement object) { return mobjects.get(object); }

    private transient Map<PropertyView, ClientPropertyView> mproperties = new HashMap<PropertyView, ClientPropertyView>();
    public ClientPropertyView get(PropertyView property) { return mproperties.get(property); }

    private transient Map<ClientGroupObjectImplement, ClientContainerView> groupObjectContainers = new HashMap<ClientGroupObjectImplement, ClientContainerView>();
    private transient Map<ClientGroupObjectImplement, ClientContainerView> panelContainers = new HashMap<ClientGroupObjectImplement, ClientContainerView>();
    private transient Map<ClientGroupObjectImplement, ClientContainerView> buttonContainers = new HashMap<ClientGroupObjectImplement, ClientContainerView>();
    private transient Map<ClientGroupObjectImplement, Map<AbstractGroup, ClientContainerView>> groupPropertyContainers = new HashMap<ClientGroupObjectImplement, Map<AbstractGroup, ClientContainerView>>();

    public DefaultClientFormView(NavigatorForm navigatorForm) {

        ClientContainerView mainContainer = new ClientContainerView();
        mainContainer.outName = "mainContainer";
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        containers.add(mainContainer);

        for (GroupObjectImplement group : (List<GroupObjectImplement>)navigatorForm.Groups) {

            ClientGroupObjectImplement clientGroup = new ClientGroupObjectImplement();
            clientGroup.ID = group.ID;
            clientGroup.singleViewType = group.singleViewType;

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            ClientContainerView groupContainer = new ClientContainerView();
            groupContainer.outName = "groupContainer " + group.get(0).caption;
            groupContainer.container = mainContainer;
            groupContainer.constraints.order = navigatorForm.Groups.indexOf(group);
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
            groupContainer.constraints.insetsInside = new Insets(0,0,4,0);

            groupObjectContainers.put(clientGroup, groupContainer);
            containers.add(groupContainer);

            ClientContainerView gridContainer = new ClientContainerView();
            gridContainer.outName = "gridContainer " + group.get(0).caption;
            gridContainer.container = groupContainer;
            gridContainer.constraints.order = 0;
            gridContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            containers.add(gridContainer);

            ClientContainerView panelContainer = new ClientContainerView();
            panelContainer.outName = "panelContainer " + group.get(0).caption;
            panelContainer.container = groupContainer;
            panelContainer.constraints.order = 1;
            panelContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;

            panelContainer.title = group.get(0).caption;

            panelContainers.put(clientGroup, panelContainer);
            containers.add(panelContainer);

            ClientContainerView buttonContainer = new ClientContainerView();
            buttonContainer.outName = "buttonContainer " + group.get(0).caption;
            buttonContainer.container = groupContainer;
            buttonContainer.constraints.order = 2;
            buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

            buttonContainers.put(clientGroup, buttonContainer);
            containers.add(buttonContainer);

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

            for (ObjectImplement object : group) {

                ClientObjectImplement clientObject = new ClientObjectImplement();
                clientObject.ID = object.ID;
                clientObject.groupObject = clientGroup;

                clientObject.objectIDView.ID = object.ID;
                clientObject.objectIDView.sID = object.getSID();
                clientObject.objectIDView.groupObject = clientGroup;
                clientObject.objectIDView.object = clientObject;

//                clientObject.objectIDView.container = panelContainer;
                clientObject.objectIDView.constraints.order = -1000 + group.indexOf(object);
                clientObject.objectIDView.constraints.insetsSibling = new Insets(0,0,2,2);

                clientObject.caption = object.caption;
                clientObject.objectIDView.caption = object.caption;

                clientObject.objectIDView.baseClass = ByteArraySerializer.deserializeClientClass(
                                           ByteArraySerializer.serializeClass(object.baseClass));

                addComponent(clientGroup, clientObject.objectIDView, object.baseClass.getParent());

                clientObject.classView.container = gridContainer;
                clientObject.classView.constraints.order = 0;
                clientObject.classView.constraints.fillVertical = 1;
                clientObject.classView.constraints.fillHorizontal = 0.2;

                clientGroup.add(clientObject);

                mobjects.put(object, clientObject);
                objects.add(clientObject);

                order.add(clientObject.objectIDView);
            }
        }

        for (PropertyView property : (List<PropertyView>)navigatorForm.propertyViews) {

            ClientGroupObjectImplement groupObject = mgroupObjects.get(property.toDraw);

            ClientPropertyView clientProperty = new ClientPropertyView();
            clientProperty.ID = property.ID;
            clientProperty.sID = property.getSID();

            clientProperty.groupObject = groupObject;
            clientProperty.constraints.order = navigatorForm.propertyViews.indexOf(property);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            //временно
            clientProperty.caption = property.view.property.caption;
            clientProperty.baseClass = ByteArraySerializer.deserializeClientClass(
                                       ByteArraySerializer.serializeClass(property.view.property.getBaseClass().getCommonClass()));

            mproperties.put(property, clientProperty);
            properties.add(clientProperty);

            addComponent(groupObject, clientProperty, property.view.property.getParent());
            order.add(clientProperty);
        }

        for (RegularFilterGroup filterGroup : (List<RegularFilterGroup>)navigatorForm.regularFilterGroups) {

            // ищем самый нижний GroupObjectImplement, к которому применяется фильтр
            GroupObjectImplement groupObject = null;
            int order = -1;
            for (RegularFilter regFilter : filterGroup.filters) {
                // Если просто кнопка - отменить фильтр
                if (regFilter.filter == null) continue;
                GroupObjectImplement propGroupObject = regFilter.filter.property.getApplyObject();
                if (propGroupObject.Order > order) {
                    order = propGroupObject.Order;
                    groupObject = propGroupObject;
                }
            }

            if (groupObject == null) continue;

            ClientRegularFilterGroupView filterGroupView = new ClientRegularFilterGroupView();
            filterGroupView.filterGroup = filterGroup;
            filterGroupView.container = buttonContainers.get(mgroupObjects.get(groupObject));
            filterGroupView.constraints.order = 3 + navigatorForm.regularFilterGroups.indexOf(filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);

            regularFilters.add(filterGroupView);
        }

        ClientContainerView formButtonContainer = new ClientContainerView();
        formButtonContainer.container = mainContainer;
        formButtonContainer.constraints.order = navigatorForm.Groups.size();
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;

        containers.add(formButtonContainer);

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

    private void addComponent(ClientGroupObjectImplement groupObject, ClientComponentView childComponent, AbstractGroup groupAbstract) {

        int order = childComponent.constraints.order;

        while (groupAbstract != null) {

            if (!groupPropertyContainers.containsKey(groupObject))
                groupPropertyContainers.put(groupObject, new HashMap<AbstractGroup, ClientContainerView>());

            ClientContainerView groupPropertyContainer;
            if (groupPropertyContainers.get(groupObject).containsKey(groupAbstract))
                groupPropertyContainer = groupPropertyContainers.get(groupObject).get(groupAbstract);
            else {

                groupPropertyContainer = new ClientContainerView();
                groupPropertyContainers.get(groupObject).put(groupAbstract, groupPropertyContainer);
                groupPropertyContainer.constraints.order = order;
                groupPropertyContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
                groupPropertyContainer.constraints.fillVertical = -1;
                groupPropertyContainer.constraints.fillHorizontal = -1;
                groupPropertyContainer.title = groupAbstract.caption;
                containers.add(groupPropertyContainer);
            }

            childComponent.container = groupPropertyContainer;
            childComponent = groupPropertyContainer;
            groupAbstract = groupAbstract.getParent();
        }

        childComponent.container = panelContainers.get(groupObject);

    }

}
