package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.ObjectMapping;
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

    // copy-constructor
    public CalculationsView(CalculationsView src, ObjectMapping mapping) {
        super(src, mapping);

        ID = BaseLogicsModule.generateStaticNewID();
    }
}
