package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;

import java.util.ArrayList;

public class SetUserFilters extends FormRequestCountingAction<ServerResponseResult> {
    public ArrayList<GPropertyFilterDTO> filters;

    public SetUserFilters() {}

    public SetUserFilters(ArrayList<GPropertyFilterDTO> filters) {
        this.filters = filters;
    }
}
