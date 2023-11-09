package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;

import java.util.List;
import java.util.Map;

public class SetUserFilters extends FormRequestCountingAction<ServerResponseResult> {
    public Map<Integer, List<GPropertyFilterDTO>> filters;

    public SetUserFilters() {}

    public SetUserFilters(Map<Integer, List<GPropertyFilterDTO>> filters) {
        this.filters = filters;
    }
}
