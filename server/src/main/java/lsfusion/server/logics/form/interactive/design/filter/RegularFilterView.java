package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.IdentityView;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterView extends IdentityView<RegularFilterView, RegularFilterEntity> {

    public RegularFilterEntity entity;

    @Override
    public int getID() {
        return entity.getID();
    }

    public String getSID() {
        return entity.getSID();
    }

    @Override
    public String toString() {
        return entity.toString();
    }

    public RegularFilterView(RegularFilterEntity entity) {
        this.entity = entity;
        this.entity.view = this;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        String name = ThreadLocalContext.localize(entity.name);
        pool.writeString(outStream, name);
        pool.writeObject(outStream, entity.keyInputEvent);
        outStream.writeBoolean(entity.showKey);
        pool.writeObject(outStream, entity.mouseInputEvent);
        outStream.writeBoolean(entity.showMouse);
    }

    // copy-constructor
    protected RegularFilterView(RegularFilterView src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);
    }

    @Override
    public RegularFilterEntity getAddParent(ObjectMapping mapping) {
        return entity;
    }
    @Override
    public RegularFilterView getAddChild(RegularFilterEntity regularFilterEntity, ObjectMapping mapping) {
        return regularFilterEntity.view;
    }
    @Override
    public RegularFilterView copy(ObjectMapping mapping) {
        return new RegularFilterView(this, mapping);
    }
}
