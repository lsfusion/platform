package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.form.shared.view.GDefaultFormsType;
import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;

public class ShowDefaultFormsResult implements Result {
    public GDefaultFormsType defaultFormsType;
    public ArrayList<String> defaultForms;

    @SuppressWarnings("UnusedDeclaration")
    public ShowDefaultFormsResult() {
    }

    public ShowDefaultFormsResult(GDefaultFormsType defaultFormsType, ArrayList<String> defaultForms) {
        this.defaultFormsType = defaultFormsType;
        this.defaultForms = defaultForms;
    }
}
