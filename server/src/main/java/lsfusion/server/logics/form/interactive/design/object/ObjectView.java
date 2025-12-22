package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.IdentityView;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView extends IdentityView<ObjectView, ObjectEntity> {

    public ObjectEntity entity;
    
    private GroupObjectView groupObject;

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

    public ObjectView(ObjectEntity entity, GroupObjectView groupTo) {
        this.entity = entity;

        this.groupObject = groupTo;
    }
    // no extend and add

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeObject(outStream, groupObject);
        pool.writeString(outStream, ThreadLocalContext.localize(entity.getCaption()));

        entity.baseClass.serialize(outStream);
    }

    public void finalizeAroundInit() {
    }

    // copy-constructor
    protected ObjectView(ObjectView src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);
        groupObject = mapping.get(src.groupObject);
    }
    // no extend and add

    @Override
    public ObjectEntity getAddParent(ObjectMapping mapping) {
        return entity;
    }
    @Override
    public ObjectView getAddChild(ObjectEntity groupObjectView, ObjectMapping mapping) {
        return groupObjectView.view;
    }
    @Override
    public ObjectView copy(ObjectMapping mapping) {
        return new ObjectView(this, mapping);
    }
}
