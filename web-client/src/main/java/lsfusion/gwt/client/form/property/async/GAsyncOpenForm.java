package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.interop.form.WindowFormType;

public class GAsyncOpenForm extends GAsyncExec {
    public String canonicalName;
    public String caption;
    public boolean forbidDuplicate;
    public boolean modal;
    public GWindowFormType type;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncOpenForm() {
    }

    public GAsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal, GWindowFormType type) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
        this.type = type;
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, String actionSID) {
        formController.asyncOpenForm(this, editContext, event, actionSID);
    }

    public GWindowFormType getWindowType(boolean canShowDockedModal) {
        if(type == GWindowFormType.DOCKED) {
            //if current form is modal, new async form can't be non-modal
            if(modal && !canShowDockedModal)
                return GWindowFormType.FLOAT;
        }
        return type;
    }

    @Override
    public void exec(GAsyncFormController asyncFormController, FormsController formsController, Event editEvent, EditContext editContext, GFormController formController) {
        formsController.asyncOpenForm(asyncFormController, this, editEvent, editContext, formController);
    }
}