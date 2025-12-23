package lsfusion.server.logics.form.interactive.design;

import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;

public abstract class IdentityView<This extends IdentityView<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> extends ServerIdentityObject<This, AddParent> implements ServerIdentitySerializable {

    public IdentityView() {
    }

    protected IdentityView(This src, ObjectMapping mapping) {
        super(src, mapping);
    }
}
