package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// сейчас полный клон GroupObjectContainerSet, потом надо рефакторить
public class TreeGroupContainerSet {

    private ContainerView boxContainer;
    private ContainerView gridContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView filtersContainer;
    private ContainerView toolbarContainer;

    public ContainerView getBoxContainer() {
        return boxContainer;
    }

    public ContainerView getGridContainer() {
        return gridContainer;
    }

    public ContainerView getPanelContainer() {
        return panelContainer;
    }

    public ContainerView getGroupContainer() {
        return groupContainer;
    }

    public ContainerView getToolbarBoxContainer() {
        return toolbarBoxContainer;
    }

    public ContainerView getToolbarLeftContainer() {
        return toolbarLeftContainer;
    }

    public ContainerView getToolbarRightContainer() {
        return toolbarRightContainer;
    }

    public ContainerView getFiltersContainer() {
        return filtersContainer;
    }

    public ContainerView getToolbarContainer() {
        return toolbarContainer;
    }

    public static TreeGroupContainerSet create(TreeGroupView treeGroup, ContainerFactory<ContainerView> factory, Version version) {
        TreeGroupContainerSet set = new TreeGroupContainerSet();
        String sid = treeGroup.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer();
        set.boxContainer.setCaption(LocalizedString.create("{form.layout.tree}"));
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));

        set.gridContainer = factory.createContainer(); // контейнер грида внутрь
        set.gridContainer.setSID(DefaultFormView.getGridBoxContainerSID(sid));

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = factory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID(sid));

        set.toolbarContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID(sid));

        set.filtersContainer = factory.createContainer(); // контейнер фильтров
        set.filtersContainer.setSID(DefaultFormView.getFilterGroupsContainerSID(sid));

        set.toolbarRightContainer = factory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID(sid));

        set.toolbarLeftContainer = factory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID(sid));

        set.boxContainer.setType(ContainerType.CONTAINERV);
        set.boxContainer.setChildrenAlignment(FlexAlignment.START);
        set.boxContainer.setAlignment(FlexAlignment.STRETCH);
        set.boxContainer.setFlex(1);
        set.boxContainer.add(treeGroup.getUserFilter(), version);
        set.boxContainer.add(set.gridContainer, version);
        set.boxContainer.add(set.toolbarBoxContainer, version);
        set.boxContainer.add(set.panelContainer, version);

        set.gridContainer.setType(ContainerType.CONTAINERH);
        set.gridContainer.setAlignment(FlexAlignment.STRETCH);
        set.gridContainer.setFlex(1);
        set.gridContainer.add(treeGroup, version);

        set.toolbarBoxContainer.setType(ContainerType.CONTAINERH);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);
        set.toolbarBoxContainer.setChildrenAlignment(FlexAlignment.START);
        set.toolbarBoxContainer.add(set.toolbarLeftContainer, version);
        set.toolbarBoxContainer.add(set.toolbarRightContainer, version);

        set.toolbarLeftContainer.setType(ContainerType.CONTAINERH);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.CENTER);
        set.toolbarLeftContainer.setChildrenAlignment(FlexAlignment.END);
        set.toolbarLeftContainer.add(treeGroup.getToolbarSystem(), version);

        set.toolbarRightContainer.setType(ContainerType.CONTAINERH);
        set.toolbarRightContainer.setAlignment(FlexAlignment.CENTER);
        set.toolbarRightContainer.setChildrenAlignment(FlexAlignment.END);
        set.toolbarRightContainer.setFlex(1);
        set.toolbarRightContainer.add(set.filtersContainer, version);
        set.toolbarRightContainer.add(set.toolbarContainer, version);

        set.filtersContainer.setType(ContainerType.CONTAINERH);
        set.filtersContainer.setAlignment(FlexAlignment.CENTER);
        set.filtersContainer.setChildrenAlignment(FlexAlignment.END);

        set.toolbarContainer.setType(ContainerType.CONTAINERH);
        set.toolbarContainer.setAlignment(FlexAlignment.CENTER);

        set.panelContainer.setType(ContainerType.CONTAINERV);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);
        set.panelContainer.setChildrenAlignment(FlexAlignment.START);
        set.panelContainer.add(set.groupContainer, version);

        set.groupContainer.setType(ContainerType.COLUMNS);
        set.groupContainer.setColumns(4);

        treeGroup.getToolbarSystem().setMargin(2);
        treeGroup.getToolbarSystem().setAlignment(FlexAlignment.CENTER);
        treeGroup.getUserFilter().setAlignment(FlexAlignment.STRETCH);


        return set;
    }
}
