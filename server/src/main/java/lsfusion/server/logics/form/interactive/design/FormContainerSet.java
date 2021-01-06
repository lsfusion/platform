package lsfusion.server.logics.form.interactive.design;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;

public class FormContainerSet {
    public static final String BOX_CONTAINER = "BOX";
        public static final String OBJECTS_CONTAINER = "OBJECTS";
        public static final String PANEL_CONTAINER = "PANEL";
            public static final String GROUP_CONTAINER = "GROUP";
        public static final String TOOLBARBOX_CONTAINER = "TOOLBARBOX";
            public static final String TOOLBARLEFT_CONTAINER = "TOOLBARLEFT";
            public static final String TOOLBARRIGHT_CONTAINER = "TOOLBARRIGHT";
                public static final String TOOLBAR_CONTAINER = "TOOLBAR";

    private ContainerView mainContainer;
    private ContainerView objectsContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
    private ContainerView toolbarBoxContainer;
    private ContainerView toolbarLeftContainer;
    private ContainerView toolbarRightContainer;
    private ContainerView toolbarContainer;

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

    public static FormContainerSet fillContainers(ContainerView mainContainer, ContainerFactory<ContainerView> contFactory, Version version) {
        FormContainerSet set = new FormContainerSet();

        set.mainContainer = mainContainer;
        set.mainContainer.setFlex(1);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH);
        
        set.objectsContainer = contFactory.createContainer();
        set.objectsContainer.setSID(DefaultFormView.getObjectsContainerSID());
        set.objectsContainer.setFlex(1);
        set.objectsContainer.setAlignment(FlexAlignment.STRETCH);

        set.panelContainer = contFactory.createContainer();
        set.panelContainer.setSID(DefaultFormView.getPanelContainerSID());
        set.panelContainer.setType(ContainerType.CONTAINERH);
        set.panelContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarBoxContainer = contFactory.createContainer();
        set.toolbarBoxContainer.setSID(DefaultFormView.getToolbarBoxContainerSID());
        set.toolbarBoxContainer.setType(ContainerType.CONTAINERH);
        set.toolbarBoxContainer.setAlignment(FlexAlignment.STRETCH);

        set.groupContainer = contFactory.createContainer();
        set.groupContainer.setSID(DefaultFormView.getGroupContainerSID(""));
        set.groupContainer.setType(ContainerType.COLUMNS);
        set.groupContainer.setColumns(2);

        set.toolbarLeftContainer = contFactory.createContainer();
        set.toolbarLeftContainer.setSID(DefaultFormView.getToolbarLeftContainerSID());
        set.toolbarLeftContainer.setType(ContainerType.CONTAINERH);
        set.toolbarLeftContainer.childrenAlignment = FlexAlignment.START;
        set.toolbarLeftContainer.setFlex(0);
        set.toolbarLeftContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarRightContainer = contFactory.createContainer();
        set.toolbarRightContainer.setSID(DefaultFormView.getToolbarRightContainerSID());
        set.toolbarRightContainer.setType(ContainerType.CONTAINERH);
        set.toolbarRightContainer.childrenAlignment = FlexAlignment.END;
        set.toolbarRightContainer.setFlex(1);
        set.toolbarRightContainer.setAlignment(FlexAlignment.STRETCH);

        set.toolbarContainer = contFactory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID());
        set.toolbarContainer.setType(ContainerType.CONTAINERH);
        set.toolbarContainer.setAlignment(FlexAlignment.CENTER);

        set.toolbarRightContainer.add(set.toolbarContainer, version);
        set.toolbarBoxContainer.add(set.toolbarLeftContainer, version);
        set.toolbarBoxContainer.add(set.toolbarRightContainer, version);
        set.mainContainer.add(set.panelContainer, version);
        set.mainContainer.add(set.objectsContainer, version);
        set.mainContainer.add(set.toolbarBoxContainer, version);
        set.panelContainer.add(set.groupContainer, version);

        return set;
    }

}
