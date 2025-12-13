package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.BaseIdentityComponentView;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.IdentityView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainersView;

import java.util.function.Function;

public abstract class BaseGroupComponentView<This extends BaseGroupComponentView<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> extends BaseIdentityComponentView<This, AddParent> {

    public BaseGroupComponentView(IDGenerator idGen) {
        super(idGen);
    }

    protected BaseGroupComponentView(This src, ObjectMapping mapping) {
        super(src, mapping);
    }
}
