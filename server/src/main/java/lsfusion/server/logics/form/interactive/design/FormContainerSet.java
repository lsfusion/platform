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

    public ContainerView getMainContainer() {
        return mainContainer;
    }
    
    public ContainerView getObjectsContainer() {
        return objectsContainer;
    }

    public ContainerView getToolbarBoxContainer() {
        return toolbarBoxContainer;
    }

    public ContainerView getPanelContainer() {
        return panelContainer;
    }

    public ContainerView getGroupContainer() {
        return groupContainer;
    }

    public ContainerView getToolbarLeftContainer() {
        return toolbarLeftContainer;
    }

    public ContainerView getToolbarRightContainer() {
        return toolbarRightContainer;
    }

    public ContainerView getToolbarContainer() {
        return toolbarContainer;
    }

    public ContainerView getPopupContainer() {
        return popupContainer;
    }

    public static FormContainerSet fillContainers(ContainerView mainContainer, ContainerFactory<ContainerView> contFactory, Version version) {
        FormContainerSet set = new FormContainerSet();

        set.mainContainer = mainContainer;
        set.mainContainer.setFlex(1);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH);
        
        set.objectsContainer = contFactory.createContainer();
        set.objectsContainer.setSID(DefaultFormView.getObjectsContainerSID());
        set.objectsContainer.setFlex(1);
        set.objectsContainer.setAlignment(FlexAlignment.STRETCH);
        set.objectsContainer.setBorder(true);
//        set.objectsContainer.setElementClass("shadow");

        set.panelContainer = contFactory.createContainer();
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID());
        set.panelContainer.setHorizontal(true);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarBoxContainer = contFactory.createContainer();
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID());
        set.toolbarBoxContainer.setHorizontal(true);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);

        set.groupContainer = contFactory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGroupContainerSID(""));
        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT);

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();

        set.toolbarLeftContainer = contFactory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID());
        set.toolbarLeftContainer.setHorizontal(true);
        set.toolbarLeftContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.END : FlexAlignment.START);
        set.toolbarLeftContainer.setFlex(toolbarTopLeft ? 1 : 0);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarRightContainer = contFactory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID());
        set.toolbarRightContainer.setHorizontal(true);
        set.toolbarRightContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.START : FlexAlignment.END);
        set.toolbarRightContainer.setFlex(toolbarTopLeft ? 0 : 1);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarContainer = contFactory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID());
        set.toolbarContainer.setHorizontal(true);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarRightContainer.add(set.toolbarContainer, version);

        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarRightContainer : set.toolbarLeftContainer, version);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarLeftContainer : set.toolbarRightContainer, version);

        set.popupContainer = contFactory.createContainer();
        set.popupContainer.setSID(DefaultFormView.getPopupContainerSID());
        set.popupContainer.setPopup(true);
        set.popupContainer.setImage("bi bi-three-dots-vertical", null);
        set.popupContainer.valueClass = "remove-btn-all-mb";

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
