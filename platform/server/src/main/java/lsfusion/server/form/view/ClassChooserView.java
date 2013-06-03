package lsfusion.server.form.view;

import lsfusion.interop.form.layout.SimplexConstraints;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClassChooserView extends ComponentView  {
    public ClassChooserView() {

    }

    public ObjectView object;
    public boolean visible = true;
    
    public ClassChooserView(int ID, ObjectEntity view, ObjectView object) {
        super(ID);
        this.visible = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).children.isEmpty();

        this.object = object;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object, serializationType);
        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
        visible = inStream.readBoolean();
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getClassChooserDefaultConstraints(super.getDefaultConstraints());
    }
}
