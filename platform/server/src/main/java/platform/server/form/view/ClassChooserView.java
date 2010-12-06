package platform.server.form.view;

import platform.interop.form.layout.GroupObjectContainerSet;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.classes.CustomClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClassChooserView extends ComponentView  {
    public ClassChooserView() {

    }

    public ObjectView object;
    public boolean show = true;
    
    public ClassChooserView(int ID, ObjectEntity view, ObjectView object) {
        super(ID);
        this.show = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).children.isEmpty();

        this.object = object;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object, serializationType);
        outStream.writeBoolean(show);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
        show = inStream.readBoolean();
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return GroupObjectContainerSet.getClassChooserDefaultConstraints(super.getDefaultConstraints());
    }
}
