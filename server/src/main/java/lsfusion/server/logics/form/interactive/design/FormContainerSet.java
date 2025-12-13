package lsfusion.server.logics.form.interactive.design;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.admin.Settings;

public class FormContainerSet {
    public static final String BOX_CONTAINER = "BOX";
        public static final String OBJECTS_CONTAINER = "OBJECTS";
        public static final String PANEL_CONTAINER = "PANEL";
            public static final String GROUP_CONTAINER = "GROUP";
        public static final String TOOLBARBOX_CONTAINER = "TOOLBARBOX";
            public static final String TOOLBARLEFT_CONTAINER = "TOOLBARLEFT";
            public static final String TOOLBARRIGHT_CONTAINER = "TOOLBARRIGHT";
                public static final String TOOLBAR_CONTAINER = "TOOLBAR";
                public static final String POPUP_CONTAINER = "POPUP";

    private ContainerView mainContainer;
    private ContainerView objectsContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView toolbarContainer;
    private ContainerView popupContainer;

    public DefaultFormView.ContainerSet getContainerSet(DefaultFormView formView, Version version) {
        return new DefaultFormView.ContainerSet(formView, formView, objectsContainer, panelContainer, groupContainer, toolbarBoxContainer, toolbarContainer, popupContainer, toolbarLeftContainer, toolbarRightContainer, null, null, null, version);
    }

    public static FormContainerSet fillContainers(ContainerView mainContainer, ContainerFactory<ContainerView> contFactory, Version version) {
        FormContainerSet set = new FormContainerSet();

        set.mainContainer = mainContainer;
        set.mainContainer.setFlex(1d, version);
        set.mainContainer.setShrink(true, version);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.mainContainer.setAlignShrink(true, version);

        set.objectsContainer = contFactory.createContainer();
        set.objectsContainer.setSID(DefaultFormView.getObjectsContainerSID());
        set.objectsContainer.setFlex(1d, version);
        set.objectsContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.objectsContainer.setBorder(true, version);
//        set.objectsContainer.setElementClass("shadow");

        set.panelContainer = contFactory.createContainer();
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID());
        set.panelContainer.setHorizontal(true, version);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarBoxContainer = contFactory.createContainer();
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID());
        set.toolbarBoxContainer.setHorizontal(true, version);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.groupContainer = contFactory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGroupContainerSID(""));
        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, version);

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();

        set.toolbarLeftContainer = contFactory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID());
        set.toolbarLeftContainer.setHorizontal(true, version);
        set.toolbarLeftContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.END : FlexAlignment.START, version);
        set.toolbarLeftContainer.setFlex(toolbarTopLeft ? 1d : 0d, version);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarRightContainer = contFactory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID());
        set.toolbarRightContainer.setHorizontal(true, version);
        set.toolbarRightContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.START : FlexAlignment.END, version);
        set.toolbarRightContainer.setFlex(toolbarTopLeft ? 0d : 1d, version);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarContainer = contFactory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID());
        set.toolbarContainer.setHorizontal(true, version);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarRightContainer.add(set.toolbarContainer, version);

        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarRightContainer : set.toolbarLeftContainer, version);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarLeftContainer : set.toolbarRightContainer, version);

        set.popupContainer = contFactory.createContainer();
        set.popupContainer.setSID(DefaultFormView.getPopupContainerSID());
        set.popupContainer.setPopup(true, version);
        set.popupContainer.setCollapsed(true, version);
        set.popupContainer.setImage("bi bi-three-dots-vertical", null, version);
        set.popupContainer.setValueClass("remove-btn-all-mb", version);

        if(toolbarTopLeft) {
            set.toolbarLeftContainer.addLast(set.popupContainer, version);
        } else {
            set.toolbarLeftContainer.add(set.popupContainer, version);
        }

        if(toolbarTopLeft) {
            set.mainContainer.add(set.toolbarBoxContainer, version);
        }

        set.mainContainer.add(set.panelContainer, version);
        set.mainContainer.add(set.objectsContainer, version);

        if(!toolbarTopLeft) {
            set.mainContainer.add(set.toolbarBoxContainer, version);
        }

        set.panelContainer.add(set.groupContainer, version);

        return set;
    }

}
