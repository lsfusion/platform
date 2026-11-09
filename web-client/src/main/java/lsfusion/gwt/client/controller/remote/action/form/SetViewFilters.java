package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;

import java.util.ArrayList;

public class SetViewFilters extends FormRequestCountingAction<ServerResponseResult> {
    public ArrayList<GPropertyFilterDTO> filters;
    public int groupObjectID;
    public Integer pageSize;

    public SetViewFilters() {}

    public SetViewFilters(int groupObjectID, ArrayList<GPropertyFilterDTO> filters, Integer pageSize) {
        this.groupObjectID = groupObjectID;
        this.filters = filters;
        this.pageSize = pageSize;
    }
}
