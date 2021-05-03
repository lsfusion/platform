package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

import java.util.ArrayList;

public interface EditManager {
    void getAsyncValues(String value, AsyncCallback<Pair<ArrayList<String>, Boolean>> callback);

    default void commitEditing(Object value) {
        commitEditing(new GUserInputResult(value), false);
    }
    void commitEditing(GUserInputResult result, boolean blurred);  // assert if blurred then editor rerender dom

    void cancelEditing();
}
