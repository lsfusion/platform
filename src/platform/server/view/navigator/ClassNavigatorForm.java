package platform.server.view.navigator;

import platform.server.logics.BusinessLogics;
import platform.server.logics.classes.RemoteClass;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;

public class ClassNavigatorForm extends NavigatorForm {

    public ClassNavigatorForm(BusinessLogics BL, RemoteClass cls) {
        super(cls.ID + 2134232, cls.caption);

        ObjectImplement object = new ObjectImplement(IDShift(1),cls);
        object.caption = cls.caption;

        GroupObjectImplement groupObject = new GroupObjectImplement(IDShift(1));

        groupObject.addObject(object);
        addGroup(groupObject);

        addPropertyView(BL.properties, BL.baseGroup, true, object);
    }
}
