package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.object.BaseGridComponentView;
import lsfusion.server.logics.form.interactive.design.object.GridPropertyView;

public class FilterControlsView<AddGridParent extends GridPropertyView<AddGridParent, ?>> extends BaseGridComponentView<FilterControlsView<AddGridParent>, AddGridParent> {

    public FilterControlsView(IDGenerator idGenerator, AddGridParent groupView) {
        super(idGenerator, groupView);
    }

    // copy-constructor
    protected FilterControlsView(FilterControlsView<AddGridParent> src, ObjectMapping mapping) {
        super(src, mapping);
    }

    @Override
    public FilterControlsView<AddGridParent> getAddChild(AddGridParent addGridParent, ObjectMapping mapping) {
        return addGridParent.filterControls;
    }
    @Override
    public FilterControlsView<AddGridParent> copy(ObjectMapping mapping) {
        return new FilterControlsView<>(this, mapping);
    }
}
