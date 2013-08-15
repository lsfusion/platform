package lsfusion.interop.form.layout;

import static lsfusion.base.ApiResourceBundle.getString;

public class FormContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    private C mainContainer;
    private C formButtonContainer;

    public C getMainContainer() {
        return mainContainer;
    }

    public C getFormButtonContainer() {
        return formButtonContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>, F extends AbstractFunction<C,T>> FormContainerSet<C,T> fillContainers(AbstractForm<C,T> form, ContainerFactory<C> contFactory) {

        FormContainerSet<C,T> set = new FormContainerSet<C,T>();

        set.mainContainer = form.getMainContainer();
        set.mainContainer.setDescription(getString("form.layout.main.container"));
        set.mainContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        set.formButtonContainer = contFactory.createContainer();
        set.formButtonContainer.setDescription(getString("form.layout.service.buttons"));
        set.formButtonContainer.setSID("functions.box");
        set.formButtonContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.formButtonContainer.getConstraints().fillHorizontal = 1.0;
        set.mainContainer.add((T)set.formButtonContainer);

        return set;
    }

}
