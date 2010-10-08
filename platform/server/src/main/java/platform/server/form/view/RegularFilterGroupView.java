package platform.server.form.view;

import platform.base.IDGenerator;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupView extends FunctionView {
    
    public RegularFilterGroupEntity entity;

    private List<RegularFilterView> filters = new ArrayList<RegularFilterView>();

    public RegularFilterGroupView(int ID, RegularFilterGroupEntity entity) {
        super(ID);
        this.entity = entity;

        for (RegularFilterEntity filterEntity : entity.filters) {
            filters.add(new RegularFilterView(filterEntity));
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(entity.ID);

        outStream.writeInt(entity.filters.size());
        for(RegularFilterEntity filter : entity.filters) {
            outStream.writeInt(filter.ID);
            outStream.writeUTF(filter.name);

            new ObjectOutputStream(outStream).writeObject(filter.key);
            outStream.writeBoolean(filter.showKey);
        }

        outStream.writeInt(entity.defaultFilter);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeInt(entity.ID);
        pool.serializeCollection(outStream, filters);
        outStream.writeInt(entity.defaultFilter);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        //todo:
        super.customDeserialize(pool, iID, inStream);

        ID = inStream.readInt();

        filters = pool.deserializeList(inStream);

//        defaultFilter = inStream.readInt();
    }
}
