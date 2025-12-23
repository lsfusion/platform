package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;

public abstract class BaseGridComponentView<This extends BaseGridComponentView<This, AddGridParent>, AddGridParent extends GridPropertyView<AddGridParent, ?>> extends BaseGroupComponentView<This, AddGridParent> {

    protected AddGridParent groupView;

    public BaseGridComponentView(IDGenerator idGenerator, AddGridParent groupView) {
        super(idGenerator);

        this.groupView = groupView;
    }

    protected BaseGridComponentView(This src, ObjectMapping mapping) {
        super(src, mapping);

        groupView = mapping.get(src.groupView);
    }

    @Override
    public AddGridParent getAddParent(ObjectMapping mapping) {
        return groupView;
    }
}
