package platform.client.form.queries;

import platform.client.form.LogicsSupplier;

public abstract class FilterController extends QueryController {
    
    public FilterController(LogicsSupplier logicsSupplier) {
        super(logicsSupplier);
    }

    protected QueryView createView() {
        return new FilterView();
    }
}
