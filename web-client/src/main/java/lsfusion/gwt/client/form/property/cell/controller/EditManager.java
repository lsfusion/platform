package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public interface EditManager {
    void getAsyncValues(String value, AsyncCallback<GFormController.GAsyncResult> callback);

    void commitEditing(GUserInputResult result, CommitReason commitReason);  // assert if blurred then editor rerender dom

    void cancelEditing(CancelReason cancelReason);

    boolean isThisCellEditing(CellEditor cellEditor);
}
