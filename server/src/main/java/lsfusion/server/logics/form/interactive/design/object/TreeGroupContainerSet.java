package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// сейчас полный клон GroupObjectContainerSet, потом надо рефакторить
public class TreeGroupContainerSet {

    private ContainerView boxContainer;
    private ContainerView filterBoxContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView filterGroupsContainer;
    private ContainerView toolbarContainer;
    private ContainerView popupContainer;

    public DefaultFormView.ContainerSet getContainerSet(DefaultFormView formView, TreeGroupView treeView, Version version) {
        return new DefaultFormView.ContainerSet(formView, treeView, boxContainer, panelContainer, groupContainer, toolbarBoxContainer, toolbarContainer, popupContainer, toolbarLeftContainer, toolbarRightContainer, filterBoxContainer, filterGroupsContainer, treeView.filtersContainer, version);
    }

    public ContainerView getBoxContainer() {
        return boxContainer;
    }
    public ContainerView getFilterBoxContainer() {
        return filterBoxContainer;
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

    public ContainerView getFilterGroupsContainer() {
        return filterGroupsContainer;
    }

    public ContainerView getToolbarContainer() {
        return toolbarContainer;
    }

    public ContainerView getPopupContainer() {
        return popupContainer;
    }

    public static TreeGroupContainerSet create(TreeGroupView treeGroup, ContainerFactory<ContainerView> factory, Version version) {
        TreeGroupContainerSet set = new TreeGroupContainerSet();
        String sid = treeGroup.getPropertyGroupContainerSID();

        set.boxContainer = factory.createContainer();
        set.boxContainer.setDebugPoint(treeGroup.entity.getDebugPoint(), version); //set debugPoint to containers that have a caption
        set.boxContainer.setSID(DefaultFormView.getBoxContainerSID(sid));
        set.boxContainer.setCaption(LocalizedString.create("{form.layout.tree}"), version);
        set.boxContainer.setName(treeGroup.getPropertyGroupContainerName(), version);

        set.filterBoxContainer = factory.createContainer();
        set.filterBoxContainer.setSID(DefaultFormView.getFilterBoxContainerSID(sid));

        set.panelContainer = factory.createContainer(); // контейнер панели
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID(sid));

        set.groupContainer = factory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGOGroupContainerSID("," + sid));

        set.toolbarBoxContainer = factory.createContainer(); // контейнер всех управляющих объектов
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID(sid));

        set.toolbarContainer = factory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID(sid));

        set.popupContainer = factory.createContainer();
        set.popupContainer.setSID(DefaultFormView.getPopupContainerSID(sid));
        set.popupContainer.setPopup(true, version);
        set.popupContainer.setCollapsed(true, version);
        set.popupContainer.setImage("bi bi-three-dots-vertical", null, version);

        set.filterGroupsContainer = factory.createContainer(); // контейнер фильтров
        set.filterGroupsContainer.setSID(DefaultFormView.getFilterGroupsContainerSID(sid));

        set.toolbarRightContainer = factory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID(sid));

        set.toolbarLeftContainer = factory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID(sid));

        set.boxContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.boxContainer.setFlex(1d, version);

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();
        if (toolbarTopLeft) {
            set.boxContainer.add(set.toolbarBoxContainer, version);
        }
        set.boxContainer.add(set.filterBoxContainer, version);
        set.boxContainer.add(treeGroup, version);
        if (!toolbarTopLeft) {
            set.boxContainer.add(set.toolbarBoxContainer, version);
        }
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH,version);
        set.boxContainer.add(set.panelContainer, version);

        // we're stretching the intermediate containers, and centering the leaf components
        set.filterBoxContainer.setHorizontal(true, version);
        set.filterBoxContainer.add(treeGroup.filtersContainer, version);
        treeGroup.filtersContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.filterBoxContainer.add(treeGroup.filterControls, version);
        treeGroup.filterControls.setAlignment(FlexAlignment.END, version);

        set.toolbarBoxContainer.setHorizontal(true, version);
        set.toolbarBoxContainer.add(set.toolbarLeftContainer, version);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.toolbarBoxContainer.add(set.toolbarRightContainer, version);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.toolbarRightContainer.setFlex(1d, version);

        set.toolbarLeftContainer.setHorizontal(true, version);
        set.toolbarLeftContainer.add(treeGroup.toolbarSystem, version);
        treeGroup.toolbarSystem.setAlignment(FlexAlignment.CENTER, version);

        set.toolbarRightContainer.setHorizontal(true, version);
        set.toolbarRightContainer.setChildrenAlignment(FlexAlignment.END, version);
        set.toolbarRightContainer.add(set.filterGroupsContainer, version);
        set.toolbarRightContainer.add(set.toolbarContainer, version);
        set.toolbarLeftContainer.add(set.popupContainer, version);

        set.filterGroupsContainer.setHorizontal(true, version);
        set.filterGroupsContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.filterGroupsContainer.setChildrenAlignment(FlexAlignment.END, version);

        set.toolbarContainer.setHorizontal(true, version);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.panelContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.panelContainer.add(set.groupContainer, version);

        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, version);

        treeGroup.toolbarSystem.setMargin(2, version);

        return set;
    }
}
