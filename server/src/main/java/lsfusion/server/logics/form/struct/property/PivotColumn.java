package lsfusion.server.logics.form.struct.property;

import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawViewOrPivotColumn;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PivotColumn implements PropertyDrawEntityOrPivotColumn, PropertyDrawViewOrPivotColumn {
    public String groupObject;

    public PivotColumn(String groupObject) {
        this.groupObject = groupObject;
    }

    @Override
    public GroupObjectEntity getToDraw(FormEntity form) {
        return form.getGroupObject(groupObject);
    }

    @Override
    public PropertyDrawViewOrPivotColumn getPropertyDrawViewOrPivotColumn(FormView formView) {
        return this;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeString(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) {
        //not used
    }
}
