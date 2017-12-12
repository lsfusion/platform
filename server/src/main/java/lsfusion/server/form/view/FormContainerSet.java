package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.logics.i18n.LocalizedString;

import static lsfusion.interop.form.layout.ContainerConstants.*;

public class FormContainerSet {

    private ContainerView mainContainer;
    private ContainerView objectsContainer;
    private ContainerView formButtonContainer;
    private ContainerView noGroupPanelContainer;
    private ContainerView noGroupPanelPropsContainer;
    private ContainerView noGroupToolbarPropsContainer;

    public ContainerView getMainContainer() {
        return mainContainer;
    }
    
    public ContainerView getObjectsContainer() {
        return objectsContainer;
    }

    public ContainerView getFormButtonContainer() {
        return formButtonContainer;
    }

    public ContainerView getNoGroupPanelContainer() {
        return noGroupPanelContainer;
    }

    public ContainerView getNoGroupPanelPropsContainer() {
        return noGroupPanelPropsContainer;
    }

    public ContainerView getNoGroupToolbarPropsContainer() {
        return noGroupToolbarPropsContainer;
    }

    public static FormContainerSet fillContainers(FormView form, ContainerFactory<ContainerView> contFactory) {
        return fillContainers(form, contFactory, ContainerAdder.<ContainerView, ComponentView, LocalizedString>DEFAULT());
    }

    public static FormContainerSet fillContainers(FormView form, ContainerFactory<ContainerView> contFactory, ContainerAdder<ContainerView, ComponentView, LocalizedString> adder) {
        FormContainerSet set = new FormContainerSet();

        set.mainContainer = form.getMainContainer();
        set.mainContainer.setDescription(LocalizedString.create("{form.layout.main.container}"));
        set.mainContainer.setFlex(1);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH);
        
        set.objectsContainer = contFactory.createContainer();
        set.objectsContainer.setSID(OBJECTS_CONTAINER);
        set.objectsContainer.setDescription(LocalizedString.create("{form.layout.objects.container}"));
        set.objectsContainer.setFlex(1);
        set.objectsContainer.setAlignment(FlexAlignment.STRETCH);

        set.noGroupPanelContainer = contFactory.createContainer();
        set.noGroupPanelContainer.setSID(NOGROUP_PANEL_CONTAINER);
        set.noGroupPanelContainer.setType(ContainerType.CONTAINERH);
        set.noGroupPanelContainer.setAlignment(FlexAlignment.STRETCH);

        set.formButtonContainer = contFactory.createContainer();
        set.formButtonContainer.setSID(FUNCTIONS_CONTAINER);
        set.formButtonContainer.setDescription(LocalizedString.create("{form.layout.service.buttons}"));
        set.formButtonContainer.setType(ContainerType.CONTAINERH);
        set.formButtonContainer.setAlignment(FlexAlignment.STRETCH);

        set.noGroupPanelPropsContainer = contFactory.createContainer();
        set.noGroupPanelPropsContainer.setSID(NOGROUP_PANEL_PROPS_CONTAINER);
        set.noGroupPanelPropsContainer.setType(ContainerType.COLUMNS);
        set.noGroupPanelPropsContainer.setColumns(2);

        set.noGroupToolbarPropsContainer = contFactory.createContainer(); // контейнер тулбара
        set.noGroupToolbarPropsContainer.setSID(ContainerConstants.NOGROUP_TOOLBAR_PROPS_CONTAINER);
        set.noGroupToolbarPropsContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.noGroupToolbarPropsContainer.setType(ContainerType.CONTAINERH);
        set.noGroupToolbarPropsContainer.setAlignment(FlexAlignment.CENTER);

        adder.add(set.mainContainer, set.objectsContainer);
        adder.add(set.mainContainer, set.noGroupPanelContainer);
        adder.add(set.mainContainer, set.formButtonContainer);
        adder.add(set.noGroupPanelContainer, set.noGroupPanelPropsContainer);

        return set;
    }

}
