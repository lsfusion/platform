package test;

import platform.server.view.navigator.NavigatorForm;
import platform.server.view.form.*;

class Test2NavigatorForm extends NavigatorForm<TestBusinessLogics> {

    Test2NavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Document);
        obj1.caption = "документ";
        ObjectImplement obj2 = new ObjectImplement(IDShift(1),BL.Article);
        obj2.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        gv2.addObject(obj2);
        addGroup(gv);
        addGroup(gv2);

        BL.fillSingleViews(obj1,this,null);
        BL.fillSingleViews(obj2,this,null);

        PropertyObjectImplement QImpl = BL.addPropertyView(this,BL.Quantity,gv2,obj1,obj2);
        BL.addPropertyView(this,BL.GP,gv2,obj1,obj2);
        BL.addPropertyView(this,BL.PrihQuantity,gv2,obj1,obj2);
        BL.addPropertyView(this,BL.RashQuantity,gv2,obj1,obj2);

        addFixedFilter(new Filter(QImpl, 5, new UserValueLink(0)));
//        BL.addPropertyView(this,BL.GSum,gv2,obj1,obj2);

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        AddFilter(new NotNullFilter(QImpl));
//        addFilter(new CompareFilter(Obj2Props.get("гр. тов"),0,new ObjectValueLink(obj1)));

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        fbv.AddOrder(Obj3Props.get("имя"));
//        fbv.AddOrder(Obj3Props.get("дата док."));
    }

}
