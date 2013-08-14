package lsfusion.interop.form.layout;

import static lsfusion.base.ApiResourceBundle.getString;

public class FormContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    public static final String FUNCTIONS_CONTAINER = "functions.box";
    public static final String NOGROUP_PANEL_CONTAINER = "nogroup.panel";
    public static final String NOGROUP_PANEL_PROPS_CONTAINER = "nogroup.panel.props";

    private C mainContainer;
    private C formButtonContainer;
    private C noGroupPanelContainer;
    private C noGroupPanelPropsContainer;

    public C getMainContainer() {
        return mainContainer;
    }

    public C getFormButtonContainer() {
        return formButtonContainer;
    }

    public C getNoGroupPanelContainer() {
        return noGroupPanelContainer;
    }

    public C getNoGroupPanelPropsContainer() {
        return noGroupPanelPropsContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>, F extends AbstractFunction<C,T>> FormContainerSet<C,T> fillContainers(AbstractForm<C,T> form, ContainerFactory<C> contFactory) {

        FormContainerSet<C,T> set = new FormContainerSet<C,T>();

        set.mainContainer = form.getMainContainer();
        set.mainContainer.setType(ContainerType.CONTAINERV);
        set.mainContainer.setDescription(getString("form.layout.main.container"));
        set.mainContainer.setChildrenAlignment(Alignment.LEADING);
        set.mainContainer.setFlex(1);
        set.mainContainer.setAlignment(FlexAlignment.STRETCH);

        set.formButtonContainer = contFactory.createContainer();
        set.formButtonContainer.setDescription(getString("form.layout.service.buttons"));
        set.formButtonContainer.setSID(FUNCTIONS_CONTAINER);
        set.formButtonContainer.setType(ContainerType.CONTAINERH);
        set.formButtonContainer.setAlignment(FlexAlignment.STRETCH);

        set.noGroupPanelContainer = contFactory.createContainer();
        set.noGroupPanelContainer.setSID(NOGROUP_PANEL_CONTAINER);
        set.noGroupPanelContainer.setType(ContainerType.CONTAINERH);
        set.noGroupPanelContainer.setAlignment(FlexAlignment.STRETCH);
        set.noGroupPanelContainer.setChildrenAlignment(Alignment.LEADING);

        set.noGroupPanelPropsContainer = contFactory.createContainer();
        set.noGroupPanelPropsContainer.setSID(NOGROUP_PANEL_PROPS_CONTAINER);
        set.noGroupPanelPropsContainer.setType(ContainerType.COLUMNS);
        set.noGroupPanelPropsContainer.setColumns(2);
        set.noGroupPanelPropsContainer.setGapX(10);
        set.noGroupPanelPropsContainer.setGapY(5);

        set.noGroupPanelContainer.add((T) set.noGroupPanelPropsContainer);

        set.mainContainer.add((T)set.noGroupPanelContainer);
        set.mainContainer.add((T)set.formButtonContainer);

        return set;
    }

}
