package platform.client.form.showtype;

import platform.client.form.ClientFormLayout;
import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientShowTypeView;

import java.io.IOException;

public abstract class ShowTypeController {

    ClientShowTypeView view;

    ShowTypeView showView;

    public ShowTypeController(ClientShowTypeView iview, GroupObjectLogicsSupplier logicsSupplier, final ClientForm form) {

        view = iview;

        showView = new ShowTypeView(logicsSupplier) {

            protected void buttonPressed(String action) {

                try {
                    if (action.equals("grid")) {
                        if (!classView)
                            form.changeClassView(logicsSupplier.getGroupObject(), true);
                        else
                            needToBeShown();
                    }
                    else if (action.equals("panel")) {
                        if (classView)
                            form.changeClassView(logicsSupplier.getGroupObject(), false);
                        else
                            needToBeShown();
                    } else {
                        needToBeHidden();
                    }
                } catch (IOException el) {
                    throw new RuntimeException("Ошибка при переключении вида", el);
                }
            }
        };
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(view, showView);
    }

    Boolean classView;

    public void changeClassView(Boolean classView) {

        if (!classView.equals(this.classView)) {

            this.classView = classView;
            showView.changeClassView(classView, fixedClassView);
            needToBeShown();
        }
    }

    protected abstract void needToBeShown();
    protected abstract void needToBeHidden();

    public void hideViews() {
    }

    public void showViews() {
    }

    Boolean fixedClassView;
    public void setFixedClassView(Boolean fixedClassView) {
        this.fixedClassView = fixedClassView;
    }
}
