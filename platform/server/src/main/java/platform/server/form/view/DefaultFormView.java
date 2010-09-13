package platform.server.form.view;

import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.layout.SingleSimplexConstraint;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.group.AbstractGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultFormView extends FormView {

    private transient Map<GroupObjectEntity, GroupObjectView> mgroupObjects = new HashMap<GroupObjectEntity, GroupObjectView>();
    public GroupObjectView get(GroupObjectEntity groupObject) { return mgroupObjects.get(groupObject); }

    private transient Map<ObjectEntity, ObjectView> mobjects = new HashMap<ObjectEntity, ObjectView>();
    public ObjectView get(ObjectEntity object) { return mobjects.get(object); }

    private transient Map<PropertyDrawEntity, PropertyDrawView> mproperties = new HashMap<PropertyDrawEntity, PropertyDrawView>();
    public PropertyDrawView get(PropertyDrawEntity property) { return mproperties.get(property); }

    private transient ContainerView mainContainer;
    public ContainerView getMainContainer() { return mainContainer; }

    private transient Map<GroupObjectView, ContainerView> panelContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getPanelContainer(GroupObjectView groupObject) { return panelContainers.get(groupObject); }

    private transient Map<GroupObjectView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectView, Map<AbstractGroup, ContainerView>>();

    private transient Map<GroupObjectView, ContainerView> groupContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectEntity groupObject) { return getGroupObjectContainer(get(groupObject)); }

    public DefaultFormView(FormEntity<?> formEntity) {

        readOnly = formEntity.isReadOnly();

        mainContainer = addContainer();
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        Map<ObjectView, ContainerView> buttonContainers = new HashMap<ObjectView, ContainerView>();
        for (GroupObjectEntity group : formEntity.groups) {

            GroupObjectView clientGroup = new GroupObjectView(idGenerator, group);

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            ContainerView groupContainer = addContainer(group.get(0).caption); // контейнер всей группы
            groupContainer.constraints.order = formEntity.groups.indexOf(group);
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
            controlsContainer.constraints.order = 10000;
            controlsContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
            controlsContainer.constraints.insetsInside = new Insets(0,0,0,0);
            controlsContainer.constraints.insetsSibling = new Insets(0,0,0,0);
            panelContainer.add(controlsContainer);

            clientGroup.grid.constraints.order = 1;
            clientGroup.grid.constraints.fillVertical = 1;
            clientGroup.grid.constraints.fillHorizontal = 1;
            gridContainer.add(clientGroup.grid);

            for (ObjectView clientObject : clientGroup) {

                clientObject.classChooser.constraints.order = 0;
                clientObject.classChooser.constraints.fillVertical = 1;
                clientObject.classChooser.constraints.fillHorizontal = 0.2;
                gridContainer.add(clientObject.classChooser);

                ContainerView buttonContainer = addContainer(); // контейнер кнопок
                buttonContainer.constraints.order = clientGroup.indexOf(clientObject);
                buttonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
                controlsContainer.add(buttonContainer);

                buttonContainers.put(clientObject, buttonContainer);

                mobjects.put(clientObject.entity, clientObject);
            }

            clientGroup.showType.constraints.order = clientGroup.size();
            clientGroup.showType.constraints.directions = new SimplexComponentDirections(0.01,0,0,0.01);
            controlsContainer.add(clientGroup.showType);
        }

        for (PropertyDrawEntity control : formEntity.propertyDraws) {

            GroupObjectView groupObject = mgroupObjects.get(control.toDraw);

            PropertyDrawView clientProperty = new PropertyDrawView(idGenerator.idShift(), control);
            clientProperty.constraints.order = formEntity.propertyDraws.indexOf(control);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            mproperties.put(control, clientProperty);
            properties.add(clientProperty);

            addComponent(groupObject, clientProperty, control.propertyObject.property.getParent());
            order.add(clientProperty);
        }

        // обработка хоткеев для экшенов
        for (PropertyDrawEntity actionControl : formEntity.actionObjectDraws) {
            ActionProperty prop = (ActionProperty) actionControl.propertyObject.property;
            if (prop.getName().equals("addAction")) {
                get(actionControl).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK);
            } else if (prop.getName().equals("deleteAction")) {
                get(actionControl).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK);
            } else if (prop.getName().equals("importFromExcel")) {
                get(actionControl).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK);
            }
        }

        for (RegularFilterGroupEntity filterGroup : formEntity.regularFilterGroups) {

            Set<ObjectEntity> groupObjects = new HashSet<ObjectEntity>();

            // ищем самый нижний GroupObjectInstance, к которому применяется фильтр
            for (RegularFilterEntity regFilter : filterGroup.filters)
                groupObjects.addAll(formEntity.getApplyObject(regFilter.filter.getObjects()));

            RegularFilterGroupView filterGroupView = new RegularFilterGroupView(idGenerator.idShift(), filterGroup);
            filterGroupView.constraints.order = 3 + formEntity.regularFilterGroups.indexOf(filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);
            buttonContainers.get(mgroupObjects.get(formEntity.getApplyObject(groupObjects)).get(0)).add(filterGroupView);

            regularFilters.add(filterGroupView);
        }

        ContainerView formButtonContainer = addContainer();
        formButtonContainer.constraints.order = 1000; // начинаем с очень большого числа, поскольку в mainContainer могут попадать и свойства, если toDraw == null
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        mainContainer.add(formButtonContainer);

        printFunction.constraints.order = 0;
        printFunction.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(printFunction);

        xlsFunction.constraints.order = 1;
        xlsFunction.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(xlsFunction);

        nullFunction.constraints.order = 2;
        nullFunction.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(nullFunction);

        refreshFunction.constraints.order = 3;
        refreshFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(refreshFunction);

        applyFunction.constraints.order = 4;
        applyFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(applyFunction);

        applyFunction.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        cancelFunction.constraints.order = 5;
        cancelFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(cancelFunction);

        okFunction.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        okFunction.constraints.order = 6;
        okFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(okFunction);

        closeFunction.constraints.order = 7;
        closeFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(closeFunction);

        caption = formEntity.caption;
    }

    private void addComponent(GroupObjectView groupObject, ComponentView childComponent, AbstractGroup groupAbstract) {

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
