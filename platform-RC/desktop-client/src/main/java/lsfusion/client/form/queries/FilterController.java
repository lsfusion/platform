package lsfusion.client.form.queries;

import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.form.GroupObjectLogicsSupplier;
import lsfusion.client.logics.ClientFilter;

public abstract class FilterController extends QueryController {

    private ClientFilter filter;

    public FilterController(GroupObjectLogicsSupplier logicsSupplier, ClientFilter filter) {
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
