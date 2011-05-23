package platform.client.form.showtype;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientShowType;
import platform.interop.ClassViewType;

import java.io.IOException;
import java.util.List;

public abstract class ShowTypeController {

    public ShowTypeView view;

    private ClassViewType classView = ClassViewType.HIDE;

    private List<ClassViewType> banClassView;
    public final ClientShowType showTypeKey;

    public ShowTypeController(ClientShowType showTypeKey, final GroupObjectLogicsSupplier logicsSupplier, final ClientFormController form) {
        this.showTypeKey = showTypeKey;
        view = new ShowTypeView() {

            protected void buttonPressed(String action) {

                try {
                    ClassViewType newClassView = ClassViewType.valueOf(action.toUpperCase());
                    if (!classView.equals(newClassView)) {
                        form.changeClassView(logicsSupplier.getGroupObject(), newClassView);
                    }
                } catch (IOException el) {
                    throw new RuntimeException("Ошибка при переключении вида", el);
                }
            }
        };
    }

    public void changeClassView(ClassViewType classView) {
        if (!classView.equals(this.classView)) {

            this.classView = classView;
            view.changeClassView(classView, banClassView);

            if (classView.equals(ClassViewType.HIDE)) {
                needToBeHidden();
            } else {
                needToBeShown();
            }
        }
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