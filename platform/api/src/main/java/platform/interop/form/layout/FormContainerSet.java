package platform.interop.form.layout;

import java.awt.*;

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
        set.mainContainer.setDescription("Главный контейнер");
        set.mainContainer.setSID("mainContainer");
        set.mainContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;

        set.formButtonContainer = contFactory.createContainer();
        set.formButtonContainer.setDescription("Служебные кнопки");
        set.formButtonContainer.setSID("serviceButtons");
        set.formButtonContainer.getConstraints().childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        set.mainContainer.add((T)set.formButtonContainer);

        F printFunction = form.getPrintFunction();
        printFunction.setCaption("Печать");
        printFunction.setType("Print");
        printFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T)printFunction);

        F editFunction = form.getEditFunction();
        editFunction.setCaption("Редактировать");
        editFunction.setType("Edit");
        editFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T) editFunction);

        F xlsFunction = form.getXlsFunction();
        xlsFunction.setCaption("Xls");
        xlsFunction.setType("Xls");
        xlsFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T)xlsFunction);

        F nullFunction = form.getNullFunction();
        nullFunction.setCaption("Сбросить");
        nullFunction.setType("Null");
        nullFunction.getConstraints().directions = new SimplexComponentDirections(0,0.01,0.01,0);
        set.formButtonContainer.add((T)nullFunction);

        F refreshFunction = form.getRefreshFunction();
        refreshFunction.setCaption("Обновить");
        refreshFunction.setType("Refresh");
        refreshFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        set.formButtonContainer.add((T)refreshFunction);

        F applyFunction = form.getApplyFunction();
        applyFunction.setCaption("Сохранить");
        applyFunction.setType("Apply");
        applyFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        applyFunction.getConstraints().insetsSibling = new Insets(0, 8, 0, 0);
        set.formButtonContainer.add((T)applyFunction);

        F cancelFunction = form.getCancelFunction();
        cancelFunction.setCaption("Отменить");
        cancelFunction.setType("Cancel");
        cancelFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        set.formButtonContainer.add((T)cancelFunction);

        F okFunction = form.getOkFunction();
        okFunction.setCaption("ОК");
        okFunction.setType("Ok");
        okFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        okFunction.getConstraints().insetsSibling = new Insets(0, 8, 0, 0);
        set.formButtonContainer.add((T)okFunction);

        F closeFunction = form.getCloseFunction();
        closeFunction.setCaption("Закрыть");
        closeFunction.setType("Close");
        closeFunction.getConstraints().directions = new SimplexComponentDirections(0,0,0.01,0.01);
        set.formButtonContainer.add((T)closeFunction);

        return set;
    }

}
