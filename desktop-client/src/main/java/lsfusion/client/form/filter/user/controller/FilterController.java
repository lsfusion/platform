package lsfusion.client.form.filter.user.controller;

import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.filter.user.view.QueryView;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.filter.user.ClientFilter;

public abstract class FilterController extends QueryController {

    private ClientFilter filter;

    public FilterController(TableController logicsSupplier, ClientFilter filter) {
        super(logicsSupplier);
        this.filter = filter;
    }

    protected QueryView createView() {
        return new FilterView(this);
    }

    public void addView(ClientFormLayout layout) {
        layout.add(filter, getView());
    }
}
