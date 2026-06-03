package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public interface EditManager {
    void getAsyncValues(String value, AsyncCallback<GFormController.GAsyncResult> callback);

    // the form's JS controller object ({changeProperty, exec, eval, change}); the CUSTOM cell editor exposes it as
    // its `form` field so editor JS reaches exec/eval/change the same way CUSTOM grid/cell-renderer views do
    JavaScriptObject getFormController();

    void commitEditing(GUserInputResult result, CommitReason commitReason);  // assert if blurred then editor rerender dom

    void cancelEditing(CancelReason cancelReason);

    boolean isThisCellEditing(CellEditor cellEditor);
}
