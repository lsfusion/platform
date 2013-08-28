package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;

public class GetDefaultFormsResult implements Result {
    public ArrayList<String> defaultFormsSIDs;

    @SuppressWarnings("UnusedDeclaration")
    public GetDefaultFormsResult() {
    }

    public GetDefaultFormsResult(ArrayList<String> defaultFormsSIDs) {
        this.defaultFormsSIDs = defaultFormsSIDs;
    }
}
