package platform.server.form.view;

import platform.base.IDGenerator;
import platform.server.form.entity.ObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectView implements ClientSerialize {

    public ObjectEntity entity;

    public ClassChooserView classChooser;

    public ObjectView(IDGenerator idGen, ObjectEntity entity) {

        this.entity = entity;

        classChooser = new ClassChooserView(idGen.idShift(), this.entity);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(entity.getID());
        outStream.writeUTF(entity.caption);
        outStream.writeBoolean(entity.addOnTransaction);

        entity.baseClass.serialize(outStream);
        classChooser.serialize(outStream);
    }
}
