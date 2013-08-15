package lsfusion.server.form.view;

import lsfusion.interop.form.layout.SimplexConstraints;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView {

    public byte minRowCount = 0;
    public boolean tabVertical = false;
    public boolean autoHide = false;

    public GroupObjectView groupObject;

    public GridView() {
        
    }
    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);

        this.groupObject = groupObject;
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getGridDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }
}
