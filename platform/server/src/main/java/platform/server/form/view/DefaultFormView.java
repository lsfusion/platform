package platform.server.form.view;

import platform.interop.form.layout.*;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.property.group.AbstractGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultFormView extends FormView {

    private transient Map<TreeGroupEntity, TreeGroupView> mtreeGroups = new HashMap<TreeGroupEntity, TreeGroupView>();
    public TreeGroupView get(TreeGroupEntity treeGroup) { return mtreeGroups.get(treeGroup); }

    private transient Map<GroupObjectEntity, GroupObjectView> mgroupObjects = new HashMap<GroupObjectEntity, GroupObjectView>();
    public GroupObjectView get(GroupObjectEntity groupObject) { return mgroupObjects.get(groupObject); }

    private transient Map<ObjectEntity, ObjectView> mobjects = new HashMap<ObjectEntity, ObjectView>();
    public ObjectView get(ObjectEntity object) { return mobjects.get(object); }

    private transient Map<PropertyDrawEntity, PropertyDrawView> mproperties = new HashMap<PropertyDrawEntity, PropertyDrawView>();
    public PropertyDrawView get(PropertyDrawEntity property) { return mproperties.get(property); }

    private transient Map<RegularFilterGroupEntity, RegularFilterGroupView> mfilters = new HashMap<RegularFilterGroupEntity, RegularFilterGroupView>();
    public RegularFilterGroupView get(RegularFilterGroupEntity filterGroup) { return mfilters.get(filterGroup); }

    public ContainerView getMainContainer() { return mainContainer; }

    private transient Map<GroupObjectView, ContainerView> panelContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getPanelContainer(GroupObjectView groupObject) { return panelContainers.get(groupObject); }
    public ContainerView getPanelContainer(GroupObjectEntity groupObject) { return getPanelContainer(get(groupObject)); }

    private transient Map<TreeGroupView, ContainerView> treeContainers = new HashMap<TreeGroupView, ContainerView>();
    public ContainerView getTreeContainer(TreeGroupView treeGroup) { return treeContainers.get(treeGroup); }
    public ContainerView getTreeContainer(TreeGroupEntity treeGroup) { return getTreeContainer(get(treeGroup)); }

    private transient Map<GroupObjectView, ContainerView> controlsContainers = new HashMap<GroupObjectView, ContainerView>();

    private transient Map<GroupObjectView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectView, Map<AbstractGroup, ContainerView>>();
    public ContainerView getGroupPropertyContainer(GroupObjectView groupObject, AbstractGroup group) {
        Map<AbstractGroup, ContainerView> groupPropertyContainer = groupPropertyContainers.get(groupObject);
        if(groupPropertyContainer!=null)
            return groupPropertyContainer.get(group);
        else
            return null;
    }
    public ContainerView getGroupPropertyContainer(GroupObjectEntity groupObject, AbstractGroup group) { return getGroupPropertyContainer(get(groupObject), group); }

    private transient Map<GroupObjectView, ContainerView> groupContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectEntity groupObject) { return getGroupObjectContainer(get(groupObject)); }

    public DefaultFormView() {
        
    }
    
    public DefaultFormView(FormEntity<?> formEntity) {
        super(formEntity);

        readOnly = entity.isReadOnly();

        ContainerFactory<ContainerView> containerFactory = new ContainerFactory<ContainerView>() {
            public ContainerView createContainer() {
                return new ContainerView(idGenerator.idShift());
            }
        };

        FunctionFactory<FunctionView> functionFactory = new FunctionFactory<FunctionView>() {
            public FunctionView createFunction() {
                return new FunctionView(idGenerator.idShift());
            }
        };

        FormContainerSet<ContainerView, ComponentView> formSet = FormContainerSet.fillContainers(this, containerFactory, functionFactory);

        Map<GroupObjectView, ContainerView> filterContainers = new HashMap<GroupObjectView, ContainerView>();
        for (GroupObjectEntity group : entity.groups) {
            GroupObjectView clientGroup = new GroupObjectView(idGenerator, group);

            mgroupObjects.put(group, clientGroup);
            groupObjects.add(clientGroup);

            GroupObjectContainerSet<ContainerView, ComponentView> groupSet = GroupObjectContainerSet.create(clientGroup, containerFactory);

            mainContainer.add(groupSet.getGroupContainer());

            groupContainers.put(clientGroup, groupSet.getGroupContainer());
            panelContainers.put(clientGroup, groupSet.getPanelContainer());
            controlsContainers.put(clientGroup, groupSet.getControlsContainer());
            filterContainers.put(clientGroup, groupSet.getFilterContainer());

            for (ObjectView clientObject : clientGroup) {
                // перемещаем classChooser в самое начало
                groupSet.getGridContainer().add(0, clientObject.classChooser);
                mobjects.put(clientObject.entity, clientObject);
            }
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {

            TreeGroupView treeGroupView = new TreeGroupView(this, treeGroup);

            mtreeGroups.put(treeGroup, treeGroupView);
            treeGroups.add(treeGroupView);

            TreeGroupContainerSet<ContainerView, ComponentView> treeSet = TreeGroupContainerSet.create(treeGroupView, containerFactory);

            //вставляем перед первым groupObject в данной treeGroup
            mainContainer.addBefore(treeSet.getContainer(), groupContainers.get(mgroupObjects.get(treeGroup.groups.get(0))));

            treeContainers.put(treeGroupView, treeSet.getContainer());
        }

        for (PropertyDrawEntity control : entity.propertyDraws) {

            PropertyDrawView clientProperty = new PropertyDrawView(control);

            GroupObjectEntity groupDraw = entity.forceDefaultDraw.get(control);
            if(groupDraw!=null) {
                clientProperty.keyBindingGroup = groupDraw;
            } else {
                groupDraw = control.getToDraw(entity);
            }
            GroupObjectView groupObject = mgroupObjects.get(groupDraw);

            mproperties.put(control, clientProperty);
            properties.add(clientProperty);

            //походу инициализируем порядки по умолчанию
            Boolean ascending = entity.defaultOrders.get(control);
            if (ascending != null) {
                defaultOrders.put(clientProperty, ascending);
            }

            addComponent(groupObject, clientProperty, control.propertyObject.property.getParent());
            order.add(clientProperty);

            control.proceedDefaultDesign(this);
        }

        for (RegularFilterGroupEntity filterGroup : entity.regularFilterGroups) {

            Set<ObjectEntity> groupObjects = new HashSet<ObjectEntity>();

            // ищем самый нижний GroupObjectInstance, к которому применяется фильтр
            for (RegularFilterEntity regFilter : filterGroup.filters) {
                groupObjects.addAll(entity.getApplyObject(regFilter.filter.getObjects()).objects);
            }

            GroupObjectEntity filterGroupObject = entity.getApplyObject(groupObjects);

            RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup);
            filterContainers.get(mgroupObjects.get(filterGroupObject)).add(filterGroupView);

            filterGroupView.keyBindingGroup = filterGroupObject;

            regularFilters.add(filterGroupView);
            mfilters.put(filterGroup, filterGroupView);
        }

        // передобавляем еще раз, чтобы управляющие кнопки оказались в конце контейнера
        for (GroupObjectEntity group : entity.groups) {
            panelContainers.get(mgroupObjects.get(group)).add(controlsContainers.get(mgroupObjects.get(group)));
        }
        mainContainer.add(formSet.getFormButtonContainer());

        caption = entity.caption;
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
                groupPropertyContainer = createContainer(groupAbstract.caption);
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
