package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;

public abstract class FilterController extends QueryController {
    
    public FilterController(GroupObjectLogicsSupplier logicsSupplier) {
        super(logicsSupplier);
    }

    protected QueryView createView() {
        return new FilterView();
    }
}
