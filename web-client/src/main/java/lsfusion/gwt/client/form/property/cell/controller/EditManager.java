package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.result.ListResult;

import java.util.ArrayList;

public interface EditManager {
    void getAsyncValues(String value, AsyncCallback<Pair<ArrayList<String>, Boolean>> callback);

    default void commitEditing(Object value) {
        commitEditing(value, false);
    }
    void commitEditing(Object value, boolean blurred);  // assert if blurred then editor rerender dom

    void cancelEditing();
}
