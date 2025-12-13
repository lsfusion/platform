package lsfusion.server.logics.form.interactive;

import lsfusion.server.logics.form.ObjectMapping;

public abstract class ServerIdentityObject<This extends ServerIdentityObject<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> implements MappingInterface<This> {

    public ServerIdentityObject() {
    }

    protected ServerIdentityObject(This src, ObjectMapping mapping) {
        mapping.put(src, (This)this);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public This get(ObjectMapping mapping) {
        return mapping.getIdentity((This)this);
    }

    public abstract AddParent getAddParent(ObjectMapping mapping);
    public abstract This getAddChild(AddParent parent, ObjectMapping mapping);

    public abstract This copy(ObjectMapping mapping);
    public void extend(This src, ObjectMapping mapping) {
    }
    public void add(This src, ObjectMapping mapping) {
    }
}
