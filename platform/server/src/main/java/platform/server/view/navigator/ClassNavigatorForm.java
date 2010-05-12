package platform.server.view.navigator;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;

public class ClassNavigatorForm extends NavigatorForm {

    public ClassNavigatorForm(BusinessLogics<?> BL, CustomClass cls) {
        super(cls.ID + 43132, cls.caption);

        ObjectNavigator object = new ObjectNavigator(IDShift(1),cls,cls.caption);

        GroupObjectNavigator groupObject = new GroupObjectNavigator(IDShift(1));
        groupObject.add(object);

        addGroup(groupObject);

        addControlView(BL.controls, BL.baseGroup, true, object);
        addControlView(BL.controls, BL.aggrGroup, true, object);
    }
}
