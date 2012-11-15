package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;

public abstract class FindController extends QueryController {
    
    public FindController(GroupObjectLogicsSupplier logicsSupplier) {
        super(logicsSupplier);
    }

    protected QueryView createView() {
        return new FindView(this);
    }
}
