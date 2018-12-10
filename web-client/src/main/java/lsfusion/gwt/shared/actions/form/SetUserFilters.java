package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.form.view.dto.GPropertyFilterDTO;

import java.util.ArrayList;

public class SetUserFilters extends FormRequestIndexCountingAction<ServerResponseResult> {
    public ArrayList<GPropertyFilterDTO> filters;

    public SetUserFilters() {}

    public SetUserFilters(ArrayList<GPropertyFilterDTO> filters) {
        this.filters = filters;
    }
}
