package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.logics.i18n.LocalizedString;

import static lsfusion.interop.form.layout.ContainerConstants.*;

public class FormContainerSet {

    private ContainerView mainContainer;
    private ContainerView formButtonContainer;
    private ContainerView noGroupPanelContainer;
    private ContainerView noGroupPanelPropsContainer;
    private ContainerView noGroupToolbarPropsContainer;

    public ContainerView getMainContainer() {
        return mainContainer;
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
        set.mainContainer.setType(ContainerType.CONTAINERV);
        set.mainContainer.setDescription(LocalizedString.create("{form.layout.main.container}"));

        set.formButtonContainer = contFactory.createContainer();
        set.formButtonContainer.setDescription(LocalizedString.create("{form.layout.service.buttons}"));
        set.formButtonContainer.setSID(FUNCTIONS_CONTAINER);

        set.noGroupPanelContainer = contFactory.createContainer();
        set.noGroupPanelContainer.setSID(NOGROUP_PANEL_CONTAINER);

        set.noGroupPanelPropsContainer = contFactory.createContainer();
        set.noGroupPanelPropsContainer.setSID(NOGROUP_PANEL_PROPS_CONTAINER);

        set.mainContainer.setChildrenAlignment(Alignment.LEADING);
        set.mainContainer.setFlex(1);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH);
        adder.add(set.mainContainer, set.noGroupPanelContainer);
        adder.add(set.mainContainer, set.formButtonContainer);

        set.formButtonContainer.setType(ContainerType.CONTAINERH);
        set.formButtonContainer.setAlignment(FlexAlignment.STRETCH);

        set.noGroupPanelContainer.setType(ContainerType.CONTAINERH);
        set.noGroupPanelContainer.setAlignment(FlexAlignment.STRETCH);
        set.noGroupPanelContainer.setChildrenAlignment(Alignment.LEADING);
        adder.add(set.noGroupPanelContainer, set.noGroupPanelPropsContainer);

        set.noGroupPanelPropsContainer.setType(ContainerType.COLUMNS);
        set.noGroupPanelPropsContainer.setColumns(2);

        set.noGroupToolbarPropsContainer = contFactory.createContainer(); // контейнер тулбара
        set.noGroupToolbarPropsContainer.setDescription(LocalizedString.create("{form.layout.toolbar.props.container}"));
        set.noGroupToolbarPropsContainer.setSID(ContainerConstants.NOGROUP_TOOLBAR_PROPS_CONTAINER);

        set.noGroupToolbarPropsContainer.setType(ContainerType.CONTAINERH);
        set.noGroupToolbarPropsContainer.setAlignment(FlexAlignment.CENTER);

        return set;
    }

}
