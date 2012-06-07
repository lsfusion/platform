package platform.server.form.view;

import platform.interop.form.layout.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.TreeGroupEntity;
import platform.server.logics.property.group.AbstractGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultFormView extends FormView {
    protected transient Map<GroupObjectView, ContainerView> panelContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getPanelContainer(GroupObjectView groupObject) { return panelContainers.get(groupObject); }
    public ContainerView getPanelContainer(GroupObjectEntity groupObject) { return getPanelContainer(get(groupObject)); }

    protected transient Map<TreeGroupView, ContainerView> treeContainers = new HashMap<TreeGroupView, ContainerView>();
    public ContainerView getTreeContainer(TreeGroupView treeGroup) { return treeContainers.get(treeGroup); }
    public ContainerView getTreeContainer(TreeGroupEntity treeGroup) { return getTreeContainer(get(treeGroup)); }

    protected final Map<GroupObjectView,ContainerView> filterContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getFilterContainer(GroupObjectView treeGroup) { return filterContainers.get(treeGroup); }
    public ContainerView getFilterContainer(GroupObjectEntity groupObject) { return getFilterContainer(get(groupObject)); }

    protected transient Map<GroupObjectView, ContainerView> controlsContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getControlsContainer(GroupObjectView treeGroup) { return controlsContainers.get(treeGroup); }
    public ContainerView getControlsContainer(GroupObjectEntity groupObject) { return getControlsContainer(get(groupObject)); }

    protected transient Map<GroupObjectView, ContainerView> gridContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGridContainer(GroupObjectView treeGroup) { return gridContainers.get(treeGroup); }
    public ContainerView getGridContainer(GroupObjectEntity groupObject) { return getGridContainer(get(groupObject)); }

    protected transient Map<GroupObjectView, Map<AbstractGroup, ContainerView>> groupPropertyContainers = new HashMap<GroupObjectView, Map<AbstractGroup, ContainerView>>();
    public ContainerView getGroupPropertyContainer(GroupObjectView groupObject, AbstractGroup group) {
        Map<AbstractGroup, ContainerView> groupPropertyContainer = groupPropertyContainers.get(groupObject);
        return groupPropertyContainer == null ? null : groupPropertyContainer.get(group);
    }
    public ContainerView getGroupPropertyContainer(GroupObjectEntity groupObject, AbstractGroup group) { return getGroupPropertyContainer(get(groupObject), group); }

    protected transient Map<GroupObjectView, ContainerView> groupContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectEntity groupObject) { return getGroupObjectContainer(get(groupObject)); }

    protected ContainerView formButtonContainer;

    private ContainerFactory<ContainerView> containerFactory = new ContainerFactory<ContainerView>() {
        public ContainerView createContainer() {
            return new ContainerView(idGenerator.idShift());
        }
    };

    public DefaultFormView() {

    }

    public DefaultFormView(FormEntity<?> formEntity) {
        this(formEntity, true);
    }

    protected DefaultFormView(FormEntity<?> formEntity, boolean applyDefaultDesign) {
        super(formEntity);

        caption = entity.caption;

        if (applyDefaultDesign) {
            FormContainerSet<ContainerView, ComponentView> formSet = FormContainerSet.fillContainers(this, containerFactory);

            for (GroupObjectView groupObject : groupObjects) {
                addGroupObjectView(groupObject);
            }

            for (TreeGroupView treeGroupView : treeGroups) {
                TreeGroupContainerSet<ContainerView, ComponentView> treeSet = TreeGroupContainerSet.create(treeGroupView, containerFactory);

                //вставляем перед первым groupObject в данной treeGroup
                mainContainer.addBefore(treeSet.getContainer(), groupContainers.get(mgroupObjects.get(treeGroupView.entity.groups.get(0))));

                treeContainers.put(treeGroupView, treeSet.getContainer());
            }

            for (PropertyDrawView propertyDraw : properties) {
                addPropertyDrawView(propertyDraw);
            }

            for (RegularFilterGroupView filterGroupView : regularFilters) {
                GroupObjectView filterGroupObject = mgroupObjects.get(filterGroupView.entity.getToDraw(entity));
                filterContainers.get(filterGroupObject).add(filterGroupView);
            }

            // передобавляем еще раз, чтобы управляющие кнопки оказались в конце контейнера
            for (GroupObjectEntity group : entity.groups) {
                panelContainers.get(mgroupObjects.get(group)).add(controlsContainers.get(mgroupObjects.get(group)));
            }
            formButtonContainer = formSet.getFormButtonContainer();
            mainContainer.add(formButtonContainer);
        }
    }

    private void addGroupObjectView(GroupObjectView groupObject) {
        GroupObjectContainerSet<ContainerView, ComponentView> groupSet = GroupObjectContainerSet.create(groupObject, containerFactory);

        mainContainer.add(groupSet.getGroupContainer());

        groupContainers.put(groupObject, groupSet.getGroupContainer());
        panelContainers.put(groupObject, groupSet.getPanelContainer());
        controlsContainers.put(groupObject, groupSet.getControlsContainer());
        filterContainers.put(groupObject, groupSet.getFilterContainer());
        gridContainers.put(groupObject, groupSet.getGridContainer());

        if (groupObject.size() == 1) {
            groupSet.getGridContainer().add(0, groupObject.get(0).classChooser);
        } else if (groupObject.size() > 1) {
            List<ContainerView> containers = new ArrayList<ContainerView>();
            for (int i = 0; i < groupObject.size() - 1; i++) {
                ContainerView container = createContainer();
                container.type = ContainerType.SPLIT_PANE_HORIZONTAL;
                container.add(groupObject.get(i).classChooser);
                containers.add(container);
            }
            containers.get(containers.size() - 1).add(groupObject.get(groupObject.size() - 1).classChooser);
            for (int i = containers.size() - 1; i > 0; i--) {
                containers.get(i - 1).add(containers.get(i));
            }
            groupSet.getGridContainer().add(0, containers.get(0));
        }
    }

    private void addPropertyDrawView(PropertyDrawView propertyDraw) {
        PropertyDrawEntity control = propertyDraw.entity;

        GroupObjectView groupObject = mgroupObjects.get(control.getToDraw(entity));
        addComponent(groupObject, propertyDraw, control.propertyObject.property.getParent());

        control.proceedDefaultDesign(propertyDraw, this);
    }

    @Override
    public GroupObjectView addGroupObjectEntity(GroupObjectEntity groupObject) {
        GroupObjectView view = super.addGroupObjectEntity(groupObject);
        addGroupObjectView(view);
        return view;
    }

    @Override
    public PropertyDrawView addPropertyDrawEntity(PropertyDrawEntity propertyDraw) {
        PropertyDrawView view = super.addPropertyDrawEntity(propertyDraw);
        addPropertyDrawView(view);
        return view;
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
