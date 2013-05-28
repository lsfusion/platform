package platform.client.form.queries;

import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientFilter;

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
