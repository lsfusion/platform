package platform.server.form.entity;

import platform.interop.ClassViewType;
import platform.interop.form.RemoteFormInterface;

import java.util.ArrayList;

public class GroupObjectEntity extends ArrayList<ObjectEntity> {

    public final int ID;

    public GroupObjectEntity(int iID) {
        ID = iID;
        assert (ID < RemoteFormInterface.GID_SHIFT);
    }

    @Override
    public boolean add(ObjectEntity objectEntity) {
        objectEntity.groupTo = this;
        return super.add(objectEntity);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public byte initClassView = ClassViewType.GRID;
    public byte banClassView = 0;
    public int pageSize = 10;
    
}
