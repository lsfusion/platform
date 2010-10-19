package platform.server.form.view;

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
    
    public ClassChooserView(int ID, ObjectEntity view, ObjectView object) {
        super(ID);
        this.show = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).children.isEmpty();

        this.object = object;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object, serializationType);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        object = pool.deserializeObject(inStream);
    }
}
