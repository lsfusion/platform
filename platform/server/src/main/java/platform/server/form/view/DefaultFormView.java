package platform.server.form.view;

import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.layout.SingleSimplexConstraint;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.property.group.AbstractGroup;

import java.awt.*;
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

    private transient Map<GroupObjectView, ContainerView> controlsContainers = new HashMap<GroupObjectView, ContainerView>();

    private transient Map<GroupObjectView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectView, Map<AbstractGroup, ContainerView>>();

    private transient Map<GroupObjectView, ContainerView> groupContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectEntity groupObject) { return getGroupObjectContainer(get(groupObject)); }

    public DefaultFormView() {
        
    }
    
    public DefaultFormView(FormEntity<?> formEntity) {
        super(formEntity.getID());

        readOnly = formEntity.isReadOnly();

        mainContainer = addContainer();
        mainContainer.description = "Главный контейнер";
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        Map<ObjectView, ContainerView> filterContainers = new HashMap<ObjectView, ContainerView>();
        for (GroupObjectEntity group : formEntity.groups) {

            GroupObjectView clientGroup = new GroupObjectView(idGenerator, group);

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            ContainerView groupContainer = addContainer(group.get(0).caption); // контейнер всей группы
            groupContainer.description = "Группа объектов";
            groupContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
            mainContainer.add(groupContainer);

            groupContainers.put(clientGroup, groupContainer);

            ContainerView gridContainer = addContainer(); // контейнер грида внутрь
            gridContainer.description = "Табличная часть";
            gridContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
            groupContainer.add(gridContainer);

            ContainerView panelContainer = addContainer(); // контейнер панели
            panelContainer.description = "Панель";
            groupContainer.add(panelContainer);

            panelContainers.put(clientGroup, panelContainer);

            ContainerView controlsContainer = addContainer(); // контейнер всех управляющих объектов
            controlsContainer.description = "Управляющие объекты";
            controlsContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
            controlsContainer.constraints.insetsInside = new Insets(0,0,0,0);
            controlsContainer.constraints.insetsSibling = new Insets(0,0,0,0);

            controlsContainers.put(clientGroup, controlsContainer);

            for (ObjectView clientObject : clientGroup) {

                clientObject.classChooser.constraints.fillVertical = 1;
                clientObject.classChooser.constraints.fillHorizontal = 0.2;
                gridContainer.add(clientObject.classChooser);

                ContainerView filterContainer = addContainer(); // контейнер фильтров
                filterContainer.description = "Контейнер фильтров";
                filterContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
                controlsContainer.add(filterContainer);

                filterContainers.put(clientObject, filterContainer);

                mobjects.put(clientObject.entity, clientObject);
            }

            clientGroup.grid.constraints.fillVertical = 1;
            clientGroup.grid.constraints.fillHorizontal = 1;
            gridContainer.add(clientGroup.grid);

            clientGroup.showType.constraints.directions = new SimplexComponentDirections(0.01,0,0,0.01);
            controlsContainer.add(clientGroup.showType);
        }

        for (PropertyDrawEntity control : formEntity.propertyDraws) {

            PropertyDrawView clientProperty = new PropertyDrawView(idGenerator.idShift(), this, control);
            clientProperty.constraints.insetsSibling = new Insets(0,0,2,2);

            GroupObjectEntity groupDraw = formEntity.forceDefaultDraw.get(control);
            if(groupDraw!=null) {
                clientProperty.keyBindingGroup = groupDraw;
            } else {
                groupDraw = control.toDraw;
            }
            GroupObjectView groupObject = mgroupObjects.get(groupDraw);

            mproperties.put(control, clientProperty);
            properties.add(clientProperty);

            addComponent(groupObject, clientProperty, control.propertyObject.property.getParent());
            order.add(clientProperty);

            control.proceedDefaultDesign(this);
        }

        for (RegularFilterGroupEntity filterGroup : formEntity.regularFilterGroups) {

            Set<ObjectEntity> groupObjects = new HashSet<ObjectEntity>();

            // ищем самый нижний GroupObjectInstance, к которому применяется фильтр
            for (RegularFilterEntity regFilter : filterGroup.filters)
                groupObjects.addAll(formEntity.getApplyObject(regFilter.filter.getObjects()));

            RegularFilterGroupView filterGroupView = new RegularFilterGroupView(idGenerator.idShift(), filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);
            filterContainers.get(mgroupObjects.get(formEntity.getApplyObject(groupObjects)).get(0)).add(filterGroupView);

            regularFilters.add(filterGroupView);
        }

        for (GroupObjectEntity group : formEntity.groups) {
            panelContainers.get(mgroupObjects.get(group)).add(controlsContainers.get(mgroupObjects.get(group)));
        }

        ContainerView formButtonContainer = addContainer();
        formButtonContainer.description = "Служебные кнопки";
        formButtonContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        mainContainer.add(formButtonContainer);

        printFunction.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(printFunction);

        xlsFunction.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(xlsFunction);

        nullFunction.constraints.directions = new SimplexComponentDirections(0,0.01,0.01,0);
        formButtonContainer.add(nullFunction);

        refreshFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(refreshFunction);

        applyFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(applyFunction);

        applyFunction.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        cancelFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(cancelFunction);

        okFunction.constraints.insetsSibling = new Insets(0, 8, 0, 0);

        okFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(okFunction);

        closeFunction.constraints.directions = new SimplexComponentDirections(0,0,0.01,0.01);
        formButtonContainer.add(closeFunction);

        caption = formEntity.caption;
    }

    private void addComponent(GroupObjectView groupObject, ComponentView childComponent, AbstractGroup groupAbstract) {

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
                groupPropertyContainers.get(groupObject).put(groupAbstract, groupPropertyContainer);
            }

            groupPropertyContainer.add(childComponent);
            childComponent = groupPropertyContainer;

            groupAbstract = groupAbstract.getParent();
        }

        // проверка на null нужна для глобальных свойств без groupObject'ов вообще
        ContainerView groupContainer = panelContainers.get(groupObject);
        ((groupContainer == null) ? mainContainer : groupContainer).add(childComponent);
    }

}
