package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;

public class CalculationsView extends BaseComponentView {
    public CalculationsView() {}

    public CalculationsView(int ID) {
        super(ID);
    }

    @Override
    protected int getDefaultWidth(FormInstanceContext context) {
        return 0;
    }
}
