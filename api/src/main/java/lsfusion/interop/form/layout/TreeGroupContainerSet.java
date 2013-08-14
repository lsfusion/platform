package lsfusion.interop.form.layout;

import static lsfusion.base.ApiResourceBundle.getString;
import static lsfusion.interop.form.layout.GroupObjectContainerSet.*;

public class TreeGroupContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    private C treeContainer;
    private C controlsContainer;
    private C rightControlsContainer;
    private C filtersContainer;
    private C toolbarPropsContainer;

    public C getTreeContainer() {
        return treeContainer;
    }

    public C getControlsContainer() {
        return controlsContainer;
    }

    public C getRightControlsContainer() {
        return rightControlsContainer;
    }

    public C getFiltersContainer() {
        return filtersContainer;
    }

    public C getToolbarPropsContainer() {
        return toolbarPropsContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> TreeGroupContainerSet<C, T> create(AbstractTreeGroup<C,T> treeGroup, ContainerFactory<C> factory) {
        TreeGroupContainerSet<C,T> set = new TreeGroupContainerSet<C,T>();

        set.treeContainer = factory.createContainer();
        set.treeContainer.setCaption(getString("form.layout.tree"));
        set.treeContainer.setDescription(getString("form.layout.tree"));
        set.treeContainer.setSID(treeGroup.getSID() + GroupObjectContainerSet.TREE_GROUP_CONTAINER);

        set.controlsContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.controlsContainer.setDescription(getString("form.layout.control.objects"));
        set.controlsContainer.setSID(treeGroup.getSID() + CONTROLS_CONTAINER);

        set.toolbarPropsContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarPropsContainer.setDescription(getString("form.layout.toolbar.props.container"));
        set.toolbarPropsContainer.setSID(treeGroup.getSID() + TOOLBAR_PROPS_CONTAINER);

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setDescription(getString("form.layout.filters.container"));
        set.filtersContainer.setSID(treeGroup.getSID() + FILTERS_CONTAINER);

        set.rightControlsContainer = factory.createContainer();
        set.rightControlsContainer.setSID(treeGroup.getSID() + CONTROLS_RIGHT_CONTAINER);

        set.treeContainer.add((T) treeGroup);
        set.treeContainer.add((T) set.controlsContainer);
        set.treeContainer.add((T) treeGroup.getFilter());

        set.controlsContainer.add((T) treeGroup.getToolbar());
        set.controlsContainer.add((T) set.rightControlsContainer);

        set.rightControlsContainer.add((T) set.filtersContainer);
        set.rightControlsContainer.add((T) set.toolbarPropsContainer);

        set.treeContainer.setType(ContainerType.CONTAINERV);
        set.treeContainer.setFlex(1);
        set.treeContainer.setAlignment(FlexAlignment.STRETCH);

        treeGroup.setFlex(1);
        treeGroup.setAlignment(FlexAlignment.STRETCH);
        treeGroup.getFilter().setAlignment(FlexAlignment.STRETCH);

        set.controlsContainer.setType(ContainerType.CONTAINERH);
        set.controlsContainer.setAlignment(FlexAlignment.STRETCH);

        set.rightControlsContainer.setType(ContainerType.CONTAINERH);
        set.rightControlsContainer.setAlignment(FlexAlignment.STRETCH);
        set.rightControlsContainer.setChildrenAlignment(Alignment.TRAILING);

        set.filtersContainer.setType(ContainerType.CONTAINERH);
        set.filtersContainer.setAlignment(FlexAlignment.STRETCH);
        set.filtersContainer.setChildrenAlignment(Alignment.TRAILING);

        set.toolbarPropsContainer.setType(ContainerType.CONTAINERH);

        return set;
    }
}
