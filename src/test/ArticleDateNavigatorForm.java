package test;

import platform.server.view.navigator.NavigatorForm;
import platform.server.view.form.*;
import platform.server.logics.classes.DataClass;
import platform.server.view.form.DefaultClientFormView;

class ArticleDateNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    ArticleDateNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Article);
        obj1.caption = "товар";

        ObjectImplement obj2 = new ObjectImplement(IDShift(1), DataClass.date);
        obj2.caption = "дата";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        addGroup(gv);

        gv2.addObject(obj2);
        addGroup(gv2);

        BL.fillSingleViews(obj1,this,null);

        PropertyObjectImplement QImpl = BL.addPropertyView(this, BL.ArtDateRash, gv2, obj2, obj1);

        addFixedFilter(new Filter(QImpl, 5, new UserValueLink(0)));

        DefaultClientFormView formView = new DefaultClientFormView(this);
//        formView.get(gv).defaultViewType = false;
//        formView.get(gv).singleViewType = true;

        richDesign = formView;
    }
}
