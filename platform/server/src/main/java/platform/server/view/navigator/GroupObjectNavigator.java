package platform.server.view.navigator;

import platform.interop.ClassViewType;
import platform.interop.form.RemoteFormInterface;

import java.util.ArrayList;

public class GroupObjectNavigator extends ArrayList<ObjectNavigator> {

    public final int ID;

    public GroupObjectNavigator(int iID) {
        ID = iID;
        assert (ID < RemoteFormInterface.GID_SHIFT);
    }

    @Override
    public boolean add(ObjectNavigator objectNavigator) {
        objectNavigator.groupTo = this;
        return super.add(objectNavigator);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public byte initClassView = ClassViewType.GRID;
    public byte banClassView = 0;
    public int pageSize = 10;
    
}
