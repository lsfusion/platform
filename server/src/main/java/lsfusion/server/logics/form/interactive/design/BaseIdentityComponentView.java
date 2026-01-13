package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;

public abstract class BaseIdentityComponentView<This extends BaseIdentityComponentView<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> extends BaseComponentView<This, AddParent> {

    public int ID;

    @Override
    public int getID() {
        return ID;
    }

    public BaseIdentityComponentView(IDGenerator idGenerator) {
        this.ID = idGenerator.id();
    }

    public BaseIdentityComponentView(This src, ObjectMapping mapping) {
        super(src, mapping);

        this.ID = mapping.id();
    }
}
