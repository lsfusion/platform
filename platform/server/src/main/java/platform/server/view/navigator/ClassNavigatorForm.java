package platform.server.view.navigator;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassNavigatorForm<T extends BusinessLogics<T>> extends NavigatorForm<T> {

    public final ObjectNavigator object;

    protected ClassNavigatorForm(T BL, CustomClass cls, int ID, String caption) {
        super(ID, caption);

        GroupObjectNavigator groupObject = new GroupObjectNavigator(IDShift(1));
        object = new ObjectNavigator(IDShift(1),cls,cls.caption);
        groupObject.add(object);
        addGroup(groupObject);

        addPropertyView(BL.properties, BL.baseGroup, true, object);
        addPropertyView(BL.properties, BL.aggrGroup, true, object);

        BL.addObjectActions(this, object);
    }

    public ClassNavigatorForm(T BL, CustomClass cls) {
        this(BL, cls, cls.ID + 43132, cls.caption);
    }
}
