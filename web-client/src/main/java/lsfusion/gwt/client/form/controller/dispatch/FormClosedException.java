package lsfusion.gwt.client.form.controller.dispatch;

import lsfusion.gwt.client.form.controller.GController;

// the failure of a request made once its form's dispatcher is closed, which sends nothing (RemoteDispatchAsync.gwtExecute):
// refused as a call on a closed form is, with nothing gone wrong to show or log (PriorityErrorHandlingCallback.showErrorMessage)
public class FormClosedException extends RuntimeException {
    public FormClosedException() {
        super(GController.CLOSED_REFUSAL);
    }
}
