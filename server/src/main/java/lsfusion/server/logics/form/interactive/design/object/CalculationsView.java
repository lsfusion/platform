package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;

public class CalculationsView extends BaseGridComponentView<CalculationsView, GridView> {

    public CalculationsView(IDGenerator idGenerator, GridView gridView) {
        super(idGenerator, gridView);
    }

    @Override
    protected int getDefaultWidth(FormInstanceContext context) {
        return 0;
    }

    // copy-constructor
    protected CalculationsView(CalculationsView src, ObjectMapping mapping) {
        super(src, mapping);
    }

    @Override
    public CalculationsView getAddChild(GridView gridView, ObjectMapping mapping) {
        return gridView.calculations;
    }

    @Override
    public CalculationsView copy(ObjectMapping mapping) {
        return new CalculationsView(this, mapping);
    }
}
