package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseIdentityComponentView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.DataOutputStream;
import java.io.IOException;

public class FilterView<P extends PropertyInterface, AddParent extends IdentityEntity<AddParent, ?>> extends BaseIdentityComponentView<FilterView<P, AddParent>, PropertyDrawView<P, AddParent>> {
    public PropertyDrawView<P, AddParent> property;

    public FilterView(IDGenerator idGenerator, PropertyDrawView<P, AddParent> property) {
        super(idGenerator);

        this.property = property;
        this.property.filter = this;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);
        pool.serializeObject(outStream, property);
    }

    // copy-constructor
    protected FilterView(FilterView<P, AddParent> src, ObjectMapping mapping) {
        super(src, mapping);

        property = mapping.get(src.property);
    }

    @Override
    public PropertyDrawView<P, AddParent> getAddParent(ObjectMapping mapping) {
        return property;
    }
    @Override
    public FilterView<P, AddParent> getAddChild(PropertyDrawView<P, AddParent> pdv, ObjectMapping mapping) {
        return pdv.filter;
    }
    @Override
    public FilterView<P, AddParent> copy(ObjectMapping mapping) {
        return new FilterView<>(this, mapping);
    }
}
