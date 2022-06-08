package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FilterView extends BaseComponentView {
    public PropertyDrawView property;

    public FilterView() {
    }

    public FilterView(int ID, PropertyDrawView property) {
        super(ID);
        this.property = property;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);
        pool.serializeObject(outStream, property);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        property = pool.deserializeObject(inStream);
    }
}
