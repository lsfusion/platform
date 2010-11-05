package platform.server.form.view;

import platform.interop.form.layout.ContainerFactory;
import platform.interop.form.layout.GroupObjectContainerSet;
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

    public ContainerView getMainContainer() { return mainContainer; }

    private transient Map<GroupObjectView, ContainerView> panelContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getPanelContainer(GroupObjectView groupObject) { return panelContainers.get(groupObject); }

    private transient Map<GroupObjectView, ContainerView> controlsContainers = new HashMap<GroupObjectView, ContainerView>();

    private transient Map<GroupObjectView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectView, Map<AbstractGroup, ContainerView>>();
    public ContainerView getGroupPropertyContainer(GroupObjectView groupObject, AbstractGroup group) { return groupPropertyContainers.get(groupObject).get(group); }

    private transient Map<GroupObjectView, ContainerView> groupContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectEntity groupObject) { return getGroupObjectContainer(get(groupObject)); }

    public DefaultFormView() {
        
    }
    
    public DefaultFormView(FormEntity<?> formEntity) {
        super(formEntity);

        readOnly = formEntity.isReadOnly();

        mainContainer = addContainer();
        mainContainer.description = "Главный контейнер";
        mainContainer.setSID("mainContainer");
        mainContainer.constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        Map<GroupObjectView, ContainerView> filterContainers = new HashMap<GroupObjectView, ContainerView>();
        for (GroupObjectEntity group : formEntity.groups) {

            GroupObjectView clientGroup = new GroupObjectView(idGenerator, group);

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            GroupObjectContainerSet<ContainerView, ComponentView> set = GroupObjectContainerSet.create(clientGroup,
                    new ContainerFactory<ContainerView>() {
                        public ContainerView createContainer() {
                            return new ContainerView(idGenerator.idShift());
                        }
                    });

            mainContainer.add(set.getGroupContainer());

            groupContainers.put(clientGroup, set.getGroupContainer());
            panelContainers.put(clientGroup, set.getPanelContainer());
            controlsContainers.put(clientGroup, set.getControlsContainer());
            filterContainers.put(clientGroup, set.getFilterContainer());

            for (ObjectView clientObject : clientGroup) {

                clientObject.classChooser.constraints.fillVertical = 1;
                clientObject.classChooser.constraints.fillHorizontal = 0.2;
                set.getGridContainer().add(0, clientObject.classChooser);

                mobjects.put(clientObject.entity, clientObject);
            }
        }

        for (PropertyDrawEntity control : formEntity.propertyDraws) {

            PropertyDrawView clientProperty = new PropertyDrawView(this, control);
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

            RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup);
            filterGroupView.constraints.insetsSibling = new Insets(0,4,2,4);
            filterContainers.get(mgroupObjects.get(formEntity.getApplyObject(groupObjects))).add(filterGroupView);

            regularFilters.add(filterGroupView);
        }

        // передобавляем еще раз, чтобы управляющие кнопки оказались в конце контейнера
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
