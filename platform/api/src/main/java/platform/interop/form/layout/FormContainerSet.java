package platform.interop.form.layout;

import java.awt.*;

import static platform.base.ApiResourceBundle.getString;

public class FormContainerSet <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {

    private C mainContainer;
    private C formButtonContainer;

    public C getMainContainer() {
        return mainContainer;
    }

    public C getFormButtonContainer() {
        return formButtonContainer;
    }

    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>, F extends AbstractFunction<C,T>> FormContainerSet<C,T> fillContainers(AbstractForm<C,T,F> form, ContainerFactory<C> contFactory) {

        FormContainerSet<C,T> set = new FormContainerSet<C,T>();

        set.mainContainer = form.getMainContainer();
        set.mainContainer.setDescription(getString("form.layout.main.container"));
        set.mainContainer.setSID("mainContainer");
        set.mainContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        set.formButtonContainer = contFactory.createContainer();
        set.formButtonContainer.setDescription(getString("form.layout.service.buttons"));
        set.formButtonContainer.setSID("serviceButtons");
        set.formButtonContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.mainContainer.add((T)set.formButtonContainer);

        F printFunction = form.getPrintFunction();
        printFunction.setCaption(getString("form.layout.print"));
        printFunction.setType("Print");
        printFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T)printFunction);

        F editFunction = form.getEditFunction();
        editFunction.setCaption(getString("form.layout.edit"));
        editFunction.setType("Edit");
        editFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T) editFunction);

        F xlsFunction = form.getXlsFunction();
        xlsFunction.setCaption(getString("form.layout.xls"));
        xlsFunction.setType("Xls");
        xlsFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T)xlsFunction);

        F nullFunction = form.getNullFunction();
        nullFunction.setCaption(getString("form.layout.reset"));
        nullFunction.setType("Null");
        nullFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T)nullFunction);

        F refreshFunction = form.getRefreshFunction();
        refreshFunction.setCaption(getString("form.layout.refresh"));
        refreshFunction.setType("Refresh");
        refreshFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        set.formButtonContainer.add((T)refreshFunction);

        F applyFunction = form.getApplyFunction();
        applyFunction.setCaption(getString("form.layout.apply"));
        applyFunction.setType("Apply");
        applyFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        applyFunction.getConstraints().insetsSibling = new Insets(0, 8, 0, 0);
        set.formButtonContainer.add((T)applyFunction);

        F cancelFunction = form.getCancelFunction();
        cancelFunction.setCaption(getString("form.layout.cancel"));
        cancelFunction.setType("Cancel");
        cancelFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        set.formButtonContainer.add((T)cancelFunction);

        F okFunction = form.getOkFunction();
        okFunction.setCaption(getString("form.layout.ok"));
        okFunction.setType("Ok");
        okFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        okFunction.getConstraints().insetsSibling = new Insets(0, 8, 0, 0);
        set.formButtonContainer.add((T)okFunction);

        F closeFunction = form.getCloseFunction();
        closeFunction.setCaption(getString("form.layout.close"));
        closeFunction.setType("Close");
        closeFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        set.formButtonContainer.add((T)closeFunction);

        return set;
    }

}
