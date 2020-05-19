package lsfusion.server.logics.form.interactive.design;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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
    private ContainerView toolbarBoxContainer;
    private ContainerView panelContainer;
    private ContainerView groupContainer;
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

    public ContainerView getToolbarContainer() {
        return toolbarContainer;
    }

    public static FormContainerSet fillContainers(FormView form, ContainerFactory<ContainerView> contFactory, Version version) {
        FormContainerSet set = new FormContainerSet();

        set.mainContainer = form.getMainContainer();
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

        set.toolbarContainer = contFactory.createContainer(); // контейнер тулбара
        set.toolbarContainer.setSID(DefaultFormView.getToolbarContainerSID());
        set.toolbarContainer.setType(ContainerType.CONTAINERH);
        set.toolbarContainer.setAlignment(FlexAlignment.CENTER);

        set.mainContainer.add(set.panelContainer, version);
        set.mainContainer.add(set.objectsContainer, version);
        set.mainContainer.add(set.toolbarBoxContainer, version);
        set.panelContainer.add(set.groupContainer, version);

        return set;
    }

}
