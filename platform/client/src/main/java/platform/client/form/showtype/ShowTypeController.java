package platform.client.form.showtype;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientShowType;
import platform.interop.ClassViewType;

import java.io.IOException;

public class ShowTypeController {

    private final ClientShowType showType;
    private final GroupObjectLogicsSupplier logicsSupplier;
    private final ClientFormController form;
    private final ShowTypeView view;

    public ShowTypeController(ClientGroupObject groupObject, final GroupObjectLogicsSupplier logicsSupplier, final ClientFormController form) {
        this.showType = groupObject.showType;
        this.logicsSupplier = logicsSupplier;
        this.form = form;

        view = new ShowTypeView(this, groupObject.banClassView);
    }

    public void addView(ClientFormLayout layout) {
        layout.add(showType, view);
    }

    void changeClassViewButtonClicked(ClassViewType newClassView) {
        try {
            form.changeClassView(logicsSupplier.getGroupObject(), newClassView);
        } catch (IOException el) {
            throw new RuntimeException(ClientResourceBundle.getString("form.showtype.error.switching.type"), el);
        }
    }

    public void update(ClassViewType newClassView) {
        view.setClassView(newClassView);
    }
}