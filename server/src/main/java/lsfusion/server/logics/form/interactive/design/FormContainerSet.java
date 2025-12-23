package lsfusion.server.logics.form.interactive.design;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.event.FormContainerEvent;
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

    public static final String FORM_PREFIX = "FORM";

    public static String getFormSID(String sID) {
        return FormContainerSet.FORM_PREFIX + " " + sID;
    }

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

    private static ContainerView createContainer(ContainerFactory<ContainerView> factory) {
        return factory.createContainer(null);
    }

    public static FormContainerSet fillContainers(ContainerView mainContainer, ContainerFactory<ContainerView> contFactory, Version version) {
        FormContainerSet set = new FormContainerSet();

        set.mainContainer = mainContainer;
        set.mainContainer.setFlex(1d, version);
        set.mainContainer.setShrink(true, version);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.mainContainer.setAlignShrink(true, version);

        set.objectsContainer = createContainer(contFactory);
        set.objectsContainer.setSID(DefaultFormView.getObjectsContainerSID());
        set.objectsContainer.setFlex(1d, version);
        set.objectsContainer.setAlignment(FlexAlignment.STRETCH, version);
        set.objectsContainer.setBorder(true, version);
//        set.objectsContainer.setElementClass("shadow");

        set.panelContainer = createContainer(contFactory);
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID());
        set.panelContainer.setHorizontal(true, version);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarBoxContainer = createContainer(contFactory);
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID());
        set.toolbarBoxContainer.setHorizontal(true, version);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.groupContainer = createContainer(contFactory);
        set.groupContainer.setSID(DefaultFormView.getGroupContainerSID(""));
        set.groupContainer.setLines(DefaultFormView.GROUP_CONTAINER_LINES_COUNT, version);

        boolean toolbarTopLeft = Settings.get().isToolbarTopLeft();

        set.toolbarLeftContainer = createContainer(contFactory);
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID());
        set.toolbarLeftContainer.setHorizontal(true, version);
        set.toolbarLeftContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.END : FlexAlignment.START, version);
        set.toolbarLeftContainer.setFlex(toolbarTopLeft ? 1d : 0d, version);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarRightContainer = createContainer(contFactory);
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID());
        set.toolbarRightContainer.setHorizontal(true, version);
        set.toolbarRightContainer.setChildrenAlignment(toolbarTopLeft ? FlexAlignment.START : FlexAlignment.END, version);
        set.toolbarRightContainer.setFlex(toolbarTopLeft ? 0d : 1d, version);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarContainer = createContainer(contFactory); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID());
        set.toolbarContainer.setHorizontal(true, version);
        set.toolbarContainer.setAlignment(FlexAlignment.STRETCH, version);

        set.toolbarRightContainer.add(set.toolbarContainer, version);

        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarRightContainer : set.toolbarLeftContainer, version);
        set.toolbarBoxContainer.add(toolbarTopLeft ? set.toolbarLeftContainer : set.toolbarRightContainer, version);

        set.popupContainer = createContainer(contFactory);
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
