package platform.client.form.showtype;

import platform.client.form.ClientFormLayout;
import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientShowTypeView;
import platform.interop.ClassViewType;

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
                        if (classView.equals(ClassViewType.PANEL))
                            form.changeClassView(logicsSupplier.getGroupObject(), ClassViewType.GRID);
                        else
                            needToBeShown();
                    }
                    else if (action.equals("panel")) {
                        if (classView.equals(ClassViewType.GRID))
                            form.changeClassView(logicsSupplier.getGroupObject(), ClassViewType.PANEL);
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

    Byte classView;

    public void changeClassView(Byte classView) {

        if (!classView.equals(this.classView)) {

            this.classView = classView;
            showView.changeClassView(classView, banClassView);
            needToBeShown();
        }
    }

    protected abstract void needToBeShown();
    protected abstract void needToBeHidden();

    public void hideViews() {
    }

    public void showViews() {
    }

    Byte banClassView;
    public void setBanClassView(Byte banClassView) {
        this.banClassView = banClassView;
    }
}