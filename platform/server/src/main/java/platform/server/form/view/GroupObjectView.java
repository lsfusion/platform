package platform.server.form.view;

import platform.base.IDGenerator;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectView extends ArrayList<ObjectView> implements ClientSerialize {

    public GroupObjectEntity entity;

    public GroupObjectView(IDGenerator idGen, GroupObjectEntity entity) {
        this.entity = entity;

        for(ObjectEntity object : this.entity)
            add(new ObjectView(idGen, object));
        
        grid = new GridView(idGen.idShift());
        showType = new ShowTypeView(idGen.idShift());
    }

    public GridView grid;
    public ShowTypeView showType;

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(entity.getID());
        outStream.writeByte(entity.banClassView);

        outStream.writeInt(size());
        for(ObjectView object : this)
            object.serialize(outStream);

        grid.serialize(outStream);
        showType.serialize(outStream);
    }
}
