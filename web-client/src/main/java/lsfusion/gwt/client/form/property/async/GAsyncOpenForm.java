package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.view.MainFrame;

public class GAsyncOpenForm extends GAsyncExec {
    public String canonicalName;
    public String caption;
    public boolean forbidDuplicate;
    public boolean modal;
    public boolean window;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncOpenForm() {
    }

    public GAsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal, boolean window) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
        this.window = window;
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, String actionSID) {
        formController.asyncOpenForm(this, editContext, event, actionSID);
    }

    public boolean isWindow(boolean canShowDockedModal) {
        //if current form is modal, new async form can't be non-modal
        return window || (modal && !canShowDockedModal);
    }

    @Override
    public void exec(GAsyncFormController asyncFormController, FormsController formsController) {
        formsController.asyncOpenForm(asyncFormController, this);
    }
}