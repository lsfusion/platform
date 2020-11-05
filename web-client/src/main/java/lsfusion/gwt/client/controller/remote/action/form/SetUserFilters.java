package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;

import java.util.ArrayList;

public class SetUserFilters extends FormRequestCountingAction<ServerResponseResult> {
    public ArrayList<GPropertyFilterDTO> filters;
    public boolean isViewFilter;

    public SetUserFilters() {}

    public SetUserFilters(ArrayList<GPropertyFilterDTO> filters, boolean isViewFilter) {
        this.filters = filters;
        this.isViewFilter = isViewFilter;
    }
}
