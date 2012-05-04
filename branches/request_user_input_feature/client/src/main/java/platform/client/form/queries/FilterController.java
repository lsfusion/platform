package platform.client.form.queries;

import platform.client.form.GroupObjectLogicsSupplier;

import javax.swing.*;

public abstract class FilterController extends QueryController {
    
    public FilterController(GroupObjectLogicsSupplier logicsSupplier) {
        super(logicsSupplier);
    }

    protected QueryView createView() {
        return new FilterView(this);
    }

    protected int getDestination() {
        if (getView().getVisibleConditionsCount() == 0) {
            return SwingConstants.LEFT;
        } else {
            return SwingConstants.BOTTOM;
        }
    }
}
