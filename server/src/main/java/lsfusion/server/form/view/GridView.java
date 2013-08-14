package lsfusion.server.form.view;

import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView {

    public boolean tabVertical = false;
    public boolean autoHide = false;

    public GroupObjectView groupObject;

    public GridView() {
        
    }

    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);
        this.groupObject = groupObject;
        flex = 1;
        alignment = FlexAlignment.STRETCH;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }
}
