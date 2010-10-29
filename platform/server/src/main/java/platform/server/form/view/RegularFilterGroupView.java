package platform.server.form.view;

import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupView extends ComponentView {
    
    public RegularFilterGroupEntity entity;

    private List<RegularFilterView> filters = new ArrayList<RegularFilterView>();

    public RegularFilterGroupView() {
        
    }
    
    public RegularFilterGroupView(int ID, RegularFilterGroupEntity entity) {
        super(ID);
        this.entity = entity;

        for (RegularFilterEntity filterEntity : entity.filters) {
            filters.add(new RegularFilterView(filterEntity));
        }
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeInt(entity.ID);
        pool.serializeCollection(outStream, filters);
        outStream.writeInt(entity.defaultFilter);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        entity = pool.context.form.getRegularFilterGroup(inStream.readInt());
        filters = pool.deserializeList(inStream);
    }
}
