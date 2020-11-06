package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;

import java.util.ArrayList;

public class SetViewFilters extends FormRequestCountingAction<ServerResponseResult> {
    public ArrayList<GPropertyFilterDTO> filters;

    public SetViewFilters() {}

    public SetViewFilters(ArrayList<GPropertyFilterDTO> filters) {
        this.filters = filters;
    }
}
