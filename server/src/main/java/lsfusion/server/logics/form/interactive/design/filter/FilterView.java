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

import static lsfusion.server.logics.form.interactive.design.object.GroupObjectContainerSet.FILTER_COMPONENT;

public class FilterView<P extends PropertyInterface, AddParent extends IdentityEntity<AddParent, ?>> extends BaseIdentityComponentView<FilterView<P, AddParent>, PropertyDrawView<P, AddParent>> {
    public PropertyDrawView<P, AddParent> property;

    public FilterView(IDGenerator idGenerator, PropertyDrawView<P, AddParent> property) {
        super(idGenerator);

        this.property = property;
        this.property.filter = this;
    }

    // its name is the one the design reaches it by, FILTER(name(i)) - by its property's name on the form, so it follows
    // the property into a form that embeds this one, renamed or not -: the name a React view has for it too
    @Override
    public String getSID() {
        return FILTER_COMPONENT + "(" + property.entity.getSID() + ")";
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
