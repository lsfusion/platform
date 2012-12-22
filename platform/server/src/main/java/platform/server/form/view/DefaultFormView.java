package platform.server.form.view;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import platform.interop.KeyStrokes;
import platform.interop.PropertyEditType;
import platform.interop.form.layout.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.TreeGroupEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.property.actions.form.FormToolbarActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.serialization.ServerIdentitySerializable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultFormView extends FormView {
    protected transient Map<GroupObjectView, ContainerView> groupContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGroupObjectContainer(GroupObjectView groupObject) { return groupContainers.get(groupObject); }
    public ContainerView getGroupObjectContainer(GroupObjectEntity groupObject) { return getGroupObjectContainer(get(groupObject)); }

    protected transient Map<GroupObjectView, ContainerView> gridContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getGridContainer(GroupObjectView treeGroup) { return gridContainers.get(treeGroup); }
    public ContainerView getGridContainer(GroupObjectEntity groupObject) { return getGridContainer(get(groupObject)); }

    protected transient Map<GroupObjectView, ContainerView> panelContainers = new HashMap<GroupObjectView, ContainerView>();
    public ContainerView getPanelContainer(GroupObjectView groupObject) { return panelContainers.get(groupObject); }
    public ContainerView getPanelContainer(GroupObjectEntity groupObject) { return getPanelContainer(get(groupObject)); }

    protected transient Map<TreeGroupView, ContainerView> treeContainers = new HashMap<TreeGroupView, ContainerView>();
    public ContainerView getTreeContainer(TreeGroupView treeGroup) { return treeContainers.get(treeGroup); }
    public ContainerView getTreeContainer(TreeGroupEntity treeGroup) { return getTreeContainer(get(treeGroup)); }

    protected transient Map<ServerIdentitySerializable, ContainerView> controlsContainers = new HashMap<ServerIdentitySerializable, ContainerView>();
    public ContainerView getControlsContainer(GroupObjectView groupObject) { return controlsContainers.get(groupObject); }
    public ContainerView getControlsContainer(TreeGroupView treeGroup) { return controlsContainers.get(treeGroup); }

    protected final Map<ServerIdentitySerializable, ContainerView> toolbarPropsContainers = new HashMap<ServerIdentitySerializable, ContainerView>();
    public ContainerView getToolbarPropsContainer(GroupObjectView groupObject) { return toolbarPropsContainers.get(groupObject); }
    public ContainerView getToolbarPropsContainer(TreeGroupView treeGroup) { return toolbarPropsContainers.get(treeGroup); }

    protected transient Map<ServerIdentitySerializable, ContainerView> rightControlsContainers = new HashMap<ServerIdentitySerializable, ContainerView>();
    public ContainerView getRightControlsContainer(GroupObjectView groupObject) { return rightControlsContainers.get(groupObject); }
    public ContainerView getRightControlsContainer(TreeGroupView treeGroup) { return rightControlsContainers.get(treeGroup); }

    protected final Map<ServerIdentitySerializable,ContainerView> filtersContainers = new HashMap<ServerIdentitySerializable, ContainerView>();
    public ContainerView getFilterContainer(GroupObjectView groupObject) { return filtersContainers.get(groupObject); }
    public ContainerView getFilterContainer(TreeGroupView treeGroup) { return filtersContainers.get(treeGroup); }

    protected transient Table<Optional<GroupObjectView>, AbstractGroup, ContainerView> groupPropertyContainers = HashBasedTable.create();
    public ContainerView getGroupPropertyContainer(GroupObjectView groupObject, AbstractGroup group) {
        return groupPropertyContainers.get(Optional.fromNullable(groupObject), group);
    }
    public ContainerView getGroupPropertyContainer(GroupObjectEntity groupObject, AbstractGroup group) { return getGroupPropertyContainer(get(groupObject), group); }

    public ContainerView formButtonContainer;

    private ContainerFactory<ContainerView> containerFactory = new ContainerFactory<ContainerView>() {
        public ContainerView createContainer() {
            return new ContainerView(idGenerator.idShift());
        }
    };

    public DefaultFormView() {
    }

    public DefaultFormView(FormEntity<?> formEntity) {
        super(formEntity);

        caption = entity.getTitle();

        FormContainerSet<ContainerView, ComponentView> formSet = FormContainerSet.fillContainers(this, containerFactory);
        setComponentSID(formSet.getFormButtonContainer(), formSet.getFormButtonContainer().getSID());

        for (GroupObjectView groupObject : groupObjects) {
            addGroupObjectView(groupObject);
        }

        for (TreeGroupView treeGroup : treeGroups) {
            addTreeGroupView(treeGroup);
        }

        for (PropertyDrawView propertyDraw : properties) {
            addPropertyDrawView(propertyDraw);
        }

        for (RegularFilterGroupView filterGroup : regularFilters) {
            addRegularFilterGroupView(filterGroup);
        }

        formButtonContainer = formSet.getFormButtonContainer();
        mainContainer.add(formButtonContainer);

        initFormButtons();
    }

    private void initFormButtons() {
        PropertyDrawView printFunction = get(entity.printActionPropertyDraw);
        setupFormButton(printFunction, new SimplexComponentDirections(0, 0.01, 0.01, 0), KeyStrokes.getPrintKeyStroke(), "print.png");

        PropertyDrawView xlsFunction = get(entity.xlsActionPropertyDraw);
        setupFormButton(xlsFunction, new SimplexComponentDirections(0, 0.01, 0.01, 0), KeyStrokes.getXlsKeyStroke(), "xls.png");

        PropertyDrawView editFunction = get(entity.editActionPropertyDraw);
        setupFormButton(editFunction, new SimplexComponentDirections(0, 0.01, 0.01, 0), KeyStrokes.getEditKeyStroke(), "editReport.png");

        PropertyDrawView nullFunction = get(entity.nullActionPropertyDraw);
        setupFormButton(nullFunction, new SimplexComponentDirections(0, 0.01, 0.01, 0), KeyStrokes.getNullKeyStroke(), null);

        PropertyDrawView refreshFunction = get(entity.refreshActionPropertyDraw);
        setupFormButton(refreshFunction, new SimplexComponentDirections(0, 0, 0.01, 0), KeyStrokes.getRefreshKeyStroke(), "refresh.png");
        refreshFunction.drawAsync = true;

        PropertyDrawView applyFunction = get(entity.applyActionPropertyDraw);
        applyFunction.getConstraints().insetsSibling = new Insets(0, 8, 0, 0);
        setupFormButton(applyFunction, new SimplexComponentDirections(0, 0, 0.01, 0), KeyStrokes.getApplyKeyStroke(), null);

        PropertyDrawView cancelFunction = get(entity.cancelActionPropertyDraw);
        // KeyStrokes.getEscape(!isModal),
        setupFormButton(cancelFunction, new SimplexComponentDirections(0, 0, 0.01, 0), KeyStrokes.getCancelKeyStroke(), null);

        PropertyDrawView okFunction = get(entity.okActionPropertyDraw);
        okFunction.getConstraints().insetsSibling = new Insets(0, 8, 0, 0);
        // KeyStrokes.getEnter(isDialog() ? 0 : InputEvent.CTRL_DOWN_MASK),
        setupFormButton(okFunction, new SimplexComponentDirections(0, 0, 0.01, 0), KeyStrokes.getOkKeyStroke(), null);

        PropertyDrawView closeFunction = get(entity.closeActionPropertyDraw);
        setupFormButton(closeFunction, new SimplexComponentDirections(0, 0, 0.01, 0), KeyStrokes.getCloseKeyStroke(), null);

        ContainerView leftControlsContainer = createContainer(null, null, "leftControls");
        leftControlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        ContainerView rightControlsContainer = createContainer(null, null, "rightControls");
        rightControlsContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        rightControlsContainer.constraints.directions = new SimplexComponentDirections(0, 0, 0.01, 0.01);

        leftControlsContainer.add(printFunction);
        leftControlsContainer.add(xlsFunction);
        leftControlsContainer.add(editFunction);
        leftControlsContainer.add(nullFunction);

        rightControlsContainer.add(refreshFunction);
        rightControlsContainer.add(applyFunction);
        rightControlsContainer.add(cancelFunction);
        rightControlsContainer.add(okFunction);
        rightControlsContainer.add(closeFunction);

        formButtonContainer.add(leftControlsContainer);
        formButtonContainer.add(rightControlsContainer);
    }

    private void setupFormButton(PropertyDrawView printFunction, SimplexComponentDirections directions, KeyStroke editKey, String iconPath) {
        printFunction.getConstraints().directions = directions;
        printFunction.editKey = editKey;
        printFunction.focusable = false;
        printFunction.entity.setEditType(PropertyEditType.EDITABLE);

        if (iconPath != null) {
            printFunction.showEditKey = false;
            printFunction.setFixedSize(FormToolbarActionProperty.BUTTON_SIZE);
            printFunction.design.setIconPath(iconPath);
        } else {
            printFunction.preferredSize = FormToolbarActionProperty.BUTTON_SIZE;
        }
    }

    private void addGroupObjectView(GroupObjectView groupObject) {
        GroupObjectContainerSet<ContainerView, ComponentView> groupSet = GroupObjectContainerSet.create(groupObject, containerFactory);

        mainContainer.add(groupSet.getGroupContainer());

        groupContainers.put(groupObject, groupSet.getGroupContainer());
        setComponentSID(groupSet.getGroupContainer(), groupSet.getGroupContainer().getSID());
        gridContainers.put(groupObject, groupSet.getGridContainer());
        setComponentSID(groupSet.getGridContainer(), groupSet.getGridContainer().getSID());
        panelContainers.put(groupObject, groupSet.getPanelContainer());
        setComponentSID(groupSet.getPanelContainer(), groupSet.getPanelContainer().getSID());

        controlsContainers.put(groupObject, groupSet.getControlsContainer());
        setComponentSID(groupSet.getControlsContainer(), groupSet.getControlsContainer().getSID());
        rightControlsContainers.put(groupObject, groupSet.getRightControlsContainer());
        setComponentSID(groupSet.getRightControlsContainer(), groupSet.getRightControlsContainer().getSID());
        filtersContainers.put(groupObject, groupSet.getFiltersContainer());
        setComponentSID(groupSet.getFiltersContainer(), groupSet.getFiltersContainer().getSID());
        toolbarPropsContainers.put(groupObject, groupSet.getToolbarPropsContainer());
        setComponentSID(groupSet.getToolbarPropsContainer(), groupSet.getToolbarPropsContainer().getSID());

        setComponentSID(groupSet.getToolbarContainer(), groupSet.getToolbarContainer().getSID());
        setComponentSID(groupSet.getShowTypeContainer(), groupSet.getShowTypeContainer().getSID());
        setComponentSID(groupSet.getFilterContainer(), groupSet.getFilterContainer().getSID());

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

    private void addTreeGroupView(TreeGroupView treeGroup) {
        TreeGroupContainerSet<ContainerView, ComponentView> treeSet = TreeGroupContainerSet.create(treeGroup, containerFactory);

        treeContainers.put(treeGroup, treeSet.getTreeContainer());
        setComponentSID(treeSet.getTreeContainer(), treeSet.getTreeContainer().getSID());
        controlsContainers.put(treeGroup, treeSet.getControlsContainer());
        setComponentSID(treeSet.getControlsContainer(), treeSet.getControlsContainer().getSID());
        rightControlsContainers.put(treeGroup, treeSet.getRightControlsContainer());
        setComponentSID(treeSet.getRightControlsContainer(), treeSet.getRightControlsContainer().getSID());
        filtersContainers.put(treeGroup, treeSet.getFiltersContainer());
        setComponentSID(treeSet.getFiltersContainer(), treeSet.getFiltersContainer().getSID());
        toolbarPropsContainers.put(treeGroup, treeSet.getToolbarPropsContainer());
        setComponentSID(treeSet.getToolbarPropsContainer(), treeSet.getToolbarPropsContainer().getSID());

        setComponentSID(treeSet.getToolbarContainer(), treeSet.getToolbarContainer().getSID());
        setComponentSID(treeSet.getFilterContainer(), treeSet.getFilterContainer().getSID());

        //вставляем перед первым groupObject в данной treeGroup
        mainContainer.addBefore(treeSet.getTreeContainer(), groupContainers.get(mgroupObjects.get(treeGroup.entity.getGroups().get(0))));
    }

    private void addPropertyDrawView(PropertyDrawView propertyDraw) {
        PropertyDrawEntity control = propertyDraw.entity;

        GroupObjectEntity groupObject = control.getToDraw(entity);
        GroupObjectView groupObjectView = mgroupObjects.get(groupObject);
        addPropertyDrawToLayout(groupObjectView, propertyDraw, control.propertyObject.property.getParent());

        control.proceedDefaultDesign(propertyDraw, this);

        if (groupObjectView != null && propertyDraw.entity.isDrawToToolbar()) {
            ContainerView propertyContainer = null;
            if (groupObject.treeGroup != null) {
                propertyContainer = getToolbarPropsContainer(mtreeGroups.get(groupObject.treeGroup));
            } else {
                propertyContainer = getToolbarPropsContainer(mgroupObjects.get(groupObject));
            }

            if (propertyDraw.preferredSize == null) {
                propertyDraw.preferredSize = new Dimension(-1, 20);
            }
            propertyDraw.getConstraints().insetsInside = new Insets(0, 2, 0, 2);
            propertyDraw.getConstraints().directions = new SimplexComponentDirections(0.01, 0, 0.0, 0.0);
            propertyContainer.add(propertyDraw);
        }
    }

    private void addRegularFilterGroupView(RegularFilterGroupView filterGroup) {
        ContainerView filterContainer = null;
        GroupObjectEntity groupObject = filterGroup.entity.getToDraw(entity);
        if (groupObject.treeGroup != null) {
            filterContainer = getFilterContainer(mtreeGroups.get(groupObject.treeGroup));
        } else {
            filterContainer = getFilterContainer(mgroupObjects.get(groupObject));
        }

        filterGroup.getConstraints().insetsInside = new Insets(0, 2, 0, 2);
        filterGroup.getConstraints().directions = new SimplexComponentDirections(0.01, 0.0, 0.0, 0.0);
        filterContainer.add(filterGroup);
    }

    @Override
    public GroupObjectView addGroupObject(GroupObjectEntity groupObject) {
        GroupObjectView view = super.addGroupObject(groupObject);
        addGroupObjectView(view);
        return view;
    }

    @Override
    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup) {
        TreeGroupView view = super.addTreeGroup(treeGroup);
        addTreeGroupView(view);
        return view;
    }

    @Override
    public PropertyDrawView addPropertyDraw(PropertyDrawEntity propertyDraw) {
        PropertyDrawView view = super.addPropertyDraw(propertyDraw);
        addPropertyDrawView(view);
        return view;
    }

    @Override
    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroup) {
        RegularFilterGroupView view = super.addRegularFilterGroup(filterGroup);
        addRegularFilterGroupView(view);
        return view;
    }

    private void addPropertyDrawToLayout(GroupObjectView groupObject, PropertyDrawView propertyDraw, AbstractGroup groupAbstract) {
        boolean addChild = true;

        ComponentView childComponent = propertyDraw;
        while (groupAbstract != null) {
            while (groupAbstract != null && !groupAbstract.createContainer) {
                groupAbstract = groupAbstract.getParent();
            } // пропускаем группы, по которым не нужно создавать контейнер

            if (groupAbstract == null) break;

            ContainerView groupPropertyContainer = groupPropertyContainers.get(Optional.fromNullable(groupObject), groupAbstract);
            boolean isNewContainer = false;
            if (groupPropertyContainer == null) {
                String groupContainerSID = getPropertyGroupContainerSID(groupObject, groupAbstract);

                ComponentView component = getComponentBySID(groupContainerSID);
                if (component != null) {
                    if (!(component instanceof ContainerView)) {
                        throw new IllegalStateException(groupContainerSID + " component isn't a container");
                    }
                    groupPropertyContainer = (ContainerView) component;
                } else {
                    groupPropertyContainer = createContainer(groupAbstract.caption, null, groupContainerSID);
                    isNewContainer = true;
                }

                groupPropertyContainers.put(Optional.fromNullable(groupObject), groupAbstract, groupPropertyContainer);
            }

            // здесь важно не трогать уже созданные контейнеры, чтобы при extend формы не происходило "перетасовывания" контейнеров
            if (addChild) {
                groupPropertyContainer.add(childComponent);
            }

            addChild = isNewContainer;
            childComponent = groupPropertyContainer;

            groupAbstract = groupAbstract.getParent();
        }

        if (addChild) {
            // проверка на null нужна для глобальных свойств без groupObject'ов вообще
            ContainerView groupContainer = panelContainers.get(groupObject);
            ((groupContainer == null) ? mainContainer : groupContainer).add(childComponent);
        }
    }

    private static String getPropertyGroupContainerSID(GroupObjectView group, AbstractGroup propertyGroup) {
        String propertyGroupSID = propertyGroup.getSID();
        if (propertyGroupSID.contains("_")) {
            String[] sids = propertyGroupSID.split("_", 2);
            propertyGroupSID = sids[1];
        }
        // todo : здесь конечно совсем хак - нужно более четкую схему сделать
//        if (lm.getGroupBySID(propertyGroupSID) != null) {
//            используем простое имя для групп данного модуля
//            propertyGroupSID = lm.transformSIDToName(propertyGroupSID);
//        }
        return (group == null ? "NOGROUP" : group.entity.getSID()) + "." + propertyGroupSID; // todo [dale]: разобраться с NOGROUP
    }
}
