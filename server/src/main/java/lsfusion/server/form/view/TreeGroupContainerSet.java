package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.logics.i18n.LocalizedString;

public class TreeGroupContainerSet {

    private ContainerView treeContainer;
    private ContainerView controlsContainer;
    private ContainerView rightControlsContainer;
    private ContainerView filtersContainer;
    private ContainerView toolbarPropsContainer;

    public ContainerView getTreeContainer() {
        return treeContainer;
    }

    public ContainerView getControlsContainer() {
        return controlsContainer;
    }

    public ContainerView getRightControlsContainer() {
        return rightControlsContainer;
    }

    public ContainerView getFiltersContainer() {
        return filtersContainer;
    }

    public ContainerView getToolbarPropsContainer() {
        return toolbarPropsContainer;
    }

    public static TreeGroupContainerSet create(TreeGroupView treeGroup, ContainerFactory<ContainerView> factory) {
        return create(treeGroup, factory, ContainerAdder.<ContainerView, ComponentView, LocalizedString>DEFAULT());
    }
    public static TreeGroupContainerSet create(TreeGroupView treeGroup, ContainerFactory<ContainerView> factory, 
                                               ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {
        TreeGroupContainerSet set = new TreeGroupContainerSet();

        set.treeContainer = factory.createContainer();
        set.treeContainer.setCaption(LocalizedString.create("{form.layout.tree}"));
        set.treeContainer.setDescription(LocalizedString.create("{form.layout.tree}"));
        set.treeContainer.setSID(treeGroup.getSID() + ContainerConstants.TREE_GROUP_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(LocalizedString.create("{form.layout.conrol.objects}"));
        set.controlsContainer.setSID(treeGroup.getSID() + ContainerConstants.CONTROLS_CONTAINER);

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.toolbarPropsContainer.setSID(treeGroup.getSID() + ContainerConstants.TOOLBAR_PROPS_CONTAINER);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(LocalizedString.create("{form.layout.filters.container}"));
        set.filtersContainer.setSID(treeGroup.getSID() + ContainerConstants.FILTERS_CONTAINER);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(treeGroup.getSID() + ContainerConstants.CONTROLS_RIGHT_CONTAINER);

        set.treeContainer.setType(ContainerType.CONTAINERV);
        set.treeContainer.setFlex(1);
        set.treeContainer.setAlignment(FlexAlignment.STRETCH);
        adder.add(set.treeContainer, treeGroup);
        adder.add(set.treeContainer, set.controlsContainer);
        adder.add(set.treeContainer, treeGroup.getFilter());

        set.controlsContainer.setType(ContainerType.CONTAINERH);
        set.controlsContainer.setAlignment(FlexAlignment.STRETCH);
        set.controlsContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.controlsContainer, treeGroup.getToolbar());
        adder.add(set.controlsContainer, set.rightControlsContainer);
        
        set.rightControlsContainer.setType(ContainerType.CONTAINERH);
        set.rightControlsContainer.setAlignment(FlexAlignment.CENTER);
        set.rightControlsContainer.setChildrenAlignment(Alignment.TRAILING);
        adder.add(set.rightControlsContainer, set.filtersContainer);
        adder.add(set.rightControlsContainer, set.toolbarPropsContainer);

        set.filtersContainer.setType(ContainerType.CONTAINERH);
        set.filtersContainer.setAlignment(FlexAlignment.CENTER);
        set.filtersContainer.setChildrenAlignment(Alignment.TRAILING);

        set.toolbarPropsContainer.setType(ContainerType.CONTAINERH);
        set.toolbarPropsContainer.setAlignment(FlexAlignment.CENTER);

        treeGroup.setFlex(1);
        treeGroup.setAlignment(FlexAlignment.STRETCH);

        treeGroup.getFilter().setAlignment(FlexAlignment.STRETCH);
        treeGroup.getToolbar().setMargin(2);

        return set;
    }
}
