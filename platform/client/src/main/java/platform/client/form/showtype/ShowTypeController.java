package platform.client.form.showtype;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientShowType;
import platform.interop.ClassViewType;

import java.io.IOException;
import java.util.List;

public abstract class ShowTypeController {

    public final ClientShowType showType;
    public final ShowTypeView view;

    private ClassViewType classView = ClassViewType.HIDE;

    private List<ClassViewType> banClassView;

    public ShowTypeController(ClientShowType showType, final GroupObjectLogicsSupplier logicsSupplier, final ClientFormController form) {
        this.showType = showType;

        view = new ShowTypeView() {
            protected void setNewClassView(ClassViewType newClassView) {
                try {
                    if (!classView.equals(newClassView)) {
                        form.changeClassView(logicsSupplier.getGroupObject(), newClassView);
                    }
                } catch (IOException el) {
                    throw new RuntimeException(ClientResourceBundle.getString("form.showtype.error.switching.type"), el);
                }
            }
        };
    }

    public void changeClassView(ClassViewType newClassView) {
        if (!newClassView.equals(classView)) {
            classView = newClassView;
            view.changeClassView(newClassView, banClassView);

            if (newClassView == ClassViewType.HIDE) {
                needToBeHidden();
            } else {
                needToBeShown();
            }
        }
    }

    public void addView(ClientFormLayout layout) {
        layout.add(showType, view);
    }

    protected abstract void needToBeShown();

    protected abstract void needToBeHidden();

    public void hideViews() {
    }

    public void showViews() {
    }

    public void setBanClassView(List<ClassViewType> banClassView) {
        this.banClassView = banClassView;
        view.changeClassView(classView, banClassView);
    }
}