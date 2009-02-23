package test;

import platform.server.view.navigator.NavigatorForm;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.GroupObjectImplement;

class SimpleNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    SimpleNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Article);
        obj1.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        addGroup(gv);

        BL.fillSingleViews(obj1,this,null);
    }

}
