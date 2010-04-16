package platform.client.form.queries;

import platform.client.form.LogicsSupplier;

public abstract class FindController extends QueryController {
    
    public FindController(LogicsSupplier logicsSupplier) {
        super(logicsSupplier);
    }

    protected QueryView createView() {
        return new FindView();
    }
}
