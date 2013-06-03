package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;

import java.util.ArrayList;

public class SetUserFilters extends FormRequestIndexCountingAction<ServerResponseResult> {
    public ArrayList<GPropertyFilterDTO> filters;

    public SetUserFilters() {}

    public SetUserFilters(ArrayList<GPropertyFilterDTO> filters) {
        this.filters = filters;
    }
}
