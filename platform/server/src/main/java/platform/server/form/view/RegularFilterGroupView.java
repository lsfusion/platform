package platform.server.form.view;

import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.entity.filter.RegularFilterEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class RegularFilterGroupView extends FunctionView {
    
    public RegularFilterGroupEntity entity;

    public RegularFilterGroupView(int ID, RegularFilterGroupEntity entity) {
        super(ID);
        this.entity = entity;
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
}
