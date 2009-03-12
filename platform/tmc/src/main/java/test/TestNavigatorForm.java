package test;

import platform.server.view.navigator.NavigatorForm;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.client.DefaultFormView;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

class TestNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    TestNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.articleGroup);
        obj1.caption = "группа товаров";
        ObjectImplement obj2 = new ObjectImplement(IDShift(1),BL.article);
        obj2.caption = "товар";
        ObjectImplement obj3 = new ObjectImplement(IDShift(1),BL.document);
        obj3.caption = "документ";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv3 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        gv2.addObject(obj2);
        gv3.addObject(obj3);
        addGroup(gv);
        addGroup(gv2);
        addGroup(gv3);

        Set<String> Obj2Set = new HashSet();
        Obj2Set.add("гр. тов");
        Set<String> Obj3Set = new HashSet();
        Obj3Set.add("имя");
        Obj3Set.add("дата док.");

        BL.fillSingleViews(obj1,this,null);
        Map<String, PropertyObjectImplement> Obj2Props = BL.fillSingleViews(obj2,this,Obj2Set);
        Map<String, PropertyObjectImplement> Obj3Props = BL.fillSingleViews(obj3,this,Obj3Set);

        PropertyObjectImplement QImpl = BL.addPropertyView(this,BL.Quantity,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.GP,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.PrihQuantity,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.RashQuantity,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.GSum,gv3,obj3,obj2);

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        AddFilter(new NotNullFilter(QImpl));
//        addFilter(new CompareFilter(Obj2Props.get("гр. тов"),0,new ObjectValueLink(obj1)));

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        fbv.AddOrder(Obj3Props.get("имя"));
//        fbv.AddOrder(Obj3Props.get("дата док."));

//        richDesign.getGroupObject()

        DefaultFormView formView = new DefaultFormView(this);
//        formView.get(gv).defaultViewType = true;
//        formView.get(gv).singleViewType = true;
//        formView.defaultOrders.put(formView.get(BL.getPropertyView(this,QImpl)), true);

        richDesign = formView;


    }

}
