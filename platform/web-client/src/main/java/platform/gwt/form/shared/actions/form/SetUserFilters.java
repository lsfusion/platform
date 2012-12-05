package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;

import java.util.List;

public class SetUserFilters extends FormRequestIndexCountingAction<ServerResponseResult> {
    public List<GPropertyFilterDTO> filters;

    public SetUserFilters() {}

    public SetUserFilters(List<GPropertyFilterDTO> filters) {
        this.filters = filters;
    }
}
