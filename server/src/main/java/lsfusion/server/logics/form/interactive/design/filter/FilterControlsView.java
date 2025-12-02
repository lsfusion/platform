package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;

public class FilterControlsView extends BaseComponentView {
    public FilterControlsView(int idShift) {
        super(idShift);
    }

    // copy-constructor
    public FilterControlsView(FilterControlsView src, ObjectMapping mapping) {
        super(src, mapping);
        this.ID = BaseLogicsModule.generateStaticNewID();
    }
}
