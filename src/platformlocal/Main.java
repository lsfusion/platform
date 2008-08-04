/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.GridBagConstraints;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.SingleFrameApplication;


public class Main extends SingleFrameApplication {
    
    Class[] ClassList;

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
  
        launch(Main.class, args);
        
        System.out.print(1<<0);
//          java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver"); 
//        java.lang.Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
//          Connection cn = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");
//        Connection cn = DriverManager.getConnection("jdbc:sqlserver://server:1433;User=sa;Password=");

//        BusinessLogics t = new BusinessLogics();
//        t.FullDBTest();
        
//        Test t = new Test();
//        t.SimpleTest(null);
    }

    @Override
    protected void startup() {

        try {

            DataAdapter Adapter = new DataAdapter("");

            TestBusinessLogics BL = new TestBusinessLogics();
            BL.FillDB(Adapter);
            BL.FillData(Adapter);

            RemoteNavigator<TestBusinessLogics> Navigator =  new RemoteNavigator(Adapter,BL,new HashMap());
            RemoteForm<TestBusinessLogics> Form = Navigator.CreateForm((NavigatorForm)Navigator.GetElements(null).get(0));
            
            RemoteNavigator<TestBusinessLogics> SwitchNavigator = Form.CreateNavigator();
            RemoteForm<TestBusinessLogics> SubForm = SwitchNavigator.CreateForm((NavigatorForm)SwitchNavigator.GetElements(null).get(0));

            show(new ClientForm(this, SubForm));
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}


class TestBusinessLogics extends BusinessLogics {

    // заполняет тестовую базу
    void FillData(DataAdapter ad) throws SQLException {
        
        ChangesSession Session = CreateSession();
        
        Integer i;
        Integer[] Articles = new Integer[6];
        for(i=0;i<Articles.length;i++) Articles[i] = AddObject(Session,ad,Article);

        Integer[] Stores = new Integer[2];
        for(i=0;i<Stores.length;i++) Stores[i] = AddObject(Session,ad,Store);
        
        Integer[] PrihDocuments = new Integer[6];
        for(i=0;i<PrihDocuments.length;i++) {
            PrihDocuments[i] = AddObject(Session,ad,PrihDocument);
            Name.ChangeProperty(Session,ad,"ПР ДОК "+i.toString(), PrihDocuments[i]);
        }

        Integer[] RashDocuments = new Integer[6];
        for(i=0;i<RashDocuments.length;i++) {
            RashDocuments[i] = AddObject(Session,ad,RashDocument);
            Name.ChangeProperty(Session,ad,"РАСХ ДОК "+i.toString(), RashDocuments[i]);
        }

        Integer[] ArticleGroups = new Integer[2];
        for(i=0;i<ArticleGroups.length;i++) ArticleGroups[i] = AddObject(Session,ad,ArticleGroup);

        Name.ChangeProperty(Session,ad,"КОЛБАСА", Articles[0]);
        Name.ChangeProperty(Session,ad,"ТВОРОГ", Articles[1]);
        Name.ChangeProperty(Session,ad,"МОЛОКО", Articles[2]);
        Name.ChangeProperty(Session,ad,"ОБУВЬ", Articles[3]);
        Name.ChangeProperty(Session,ad,"ДЖЕМПЕР", Articles[4]);
        Name.ChangeProperty(Session,ad,"МАЙКА", Articles[5]);

        Name.ChangeProperty(Session,ad,"СКЛАД", Stores[0]);
        Name.ChangeProperty(Session,ad,"ТЗАЛ", Stores[1]);

        Name.ChangeProperty(Session,ad,"ПРОДУКТЫ", ArticleGroups[0]);
        Name.ChangeProperty(Session,ad,"ОДЕЖДА", ArticleGroups[1]);

        DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[0]);
        DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[1]);
        DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[2]);
        DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[3]);
        DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[4]);

        DocStore.ChangeProperty(Session,ad,Stores[1],RashDocuments[0]);
        DocStore.ChangeProperty(Session,ad,Stores[1],RashDocuments[1]);
        DocStore.ChangeProperty(Session,ad,Stores[0],RashDocuments[2]);
        DocStore.ChangeProperty(Session,ad,Stores[0],RashDocuments[3]);
        DocStore.ChangeProperty(Session,ad,Stores[1],RashDocuments[4]);

//        DocStore.ChangeProperty(ad,Stores[1],Documents[5]);

        DocDate.ChangeProperty(Session,ad,1001,PrihDocuments[0]);
        DocDate.ChangeProperty(Session,ad,1001,RashDocuments[0]);
        DocDate.ChangeProperty(Session,ad,1008,PrihDocuments[1]);
        DocDate.ChangeProperty(Session,ad,1009,RashDocuments[1]);
        DocDate.ChangeProperty(Session,ad,1010,RashDocuments[2]);
        DocDate.ChangeProperty(Session,ad,1011,RashDocuments[3]);
        DocDate.ChangeProperty(Session,ad,1012,PrihDocuments[2]);
        DocDate.ChangeProperty(Session,ad,1014,PrihDocuments[3]);
        DocDate.ChangeProperty(Session,ad,1016,RashDocuments[4]);
        DocDate.ChangeProperty(Session,ad,1018,PrihDocuments[4]);
        
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[0]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[1]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[2]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[3]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[4]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[5]);

        // Quantity
        PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[0],Articles[0]);
        PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[2],Articles[0]);
        RashQuantity.ChangeProperty(Session,ad,5,RashDocuments[0],Articles[0]);
        RashQuantity.ChangeProperty(Session,ad,3,RashDocuments[1],Articles[0]);
        
        PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[0],Articles[1]);
        PrihQuantity.ChangeProperty(Session,ad,2,PrihDocuments[1],Articles[1]);
        PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[3],Articles[1]);
        RashQuantity.ChangeProperty(Session,ad,14,RashDocuments[2],Articles[1]);

        PrihQuantity.ChangeProperty(Session,ad,32,PrihDocuments[2],Articles[2]);
        PrihQuantity.ChangeProperty(Session,ad,18,PrihDocuments[3],Articles[2]);
        RashQuantity.ChangeProperty(Session,ad,2,RashDocuments[1],Articles[2]);
        RashQuantity.ChangeProperty(Session,ad,10,RashDocuments[3],Articles[2]);
        PrihQuantity.ChangeProperty(Session,ad,4,PrihDocuments[4],Articles[2]);

        PrihQuantity.ChangeProperty(Session,ad,4,PrihDocuments[3],Articles[3]);

        PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[0],Articles[4]);
        RashQuantity.ChangeProperty(Session,ad,4,RashDocuments[2],Articles[4]);
        RashQuantity.ChangeProperty(Session,ad,4,RashDocuments[3],Articles[4]);

        PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[3],Articles[5]);

        Apply(ad,Session);

    }
    
    PropertyObjectImplement AddPropView(RemoteForm fbv,LP ListProp,GroupObjectImplement gv,ObjectImplement... Params) {
        PropertyObjectImplement PropImpl = new PropertyObjectImplement((ObjectProperty)ListProp.Property);
        
        ListIterator<PropertyInterface> i = ListProp.ListInterfaces.listIterator();
        for(ObjectImplement Object : Params) {
            PropImpl.Mapping.put(i.next(),Object);
        }
        fbv.Properties.add(new PropertyView(PropImpl,gv));
        return PropImpl;
    }

    Class ArticleGroup;
    Class Document;
    Class Article;
    Class Store;
    Class PrihDocument;
    Class RashDocument;

    void InitClasses() {
        
        Article = new ObjectClass(4);
        Article.AddParent(BaseClass);
        Store = new ObjectClass(5);
        Store.AddParent(BaseClass);
        Document = new ObjectClass(6);
        Document.AddParent(BaseClass);
        PrihDocument = new ObjectClass(7);
        PrihDocument.AddParent(Document);
        RashDocument = new ObjectClass(8);
        RashDocument.AddParent(Document);
        ArticleGroup = new ObjectClass(9);
        ArticleGroup.AddParent(BaseClass);
    }

    LDP Name,DocStore,PrihQuantity,RashQuantity,ArtToGroup,
            DocDate,GrAddV,ArtAddV,BarCode;
    LRP FilledProperty,Quantity,OstArtStore,MaxOpStore,OpValue;
    LGP GP,GSum,GAP,G2P,OstArt,MaxPrih,SumMaxArt;

    void InitProperties() {
        
        Name = AddDProp(StringClass,BaseClass);
        DocStore = AddDProp(Store,Document);
        PrihQuantity = AddDProp(IntegerClass,PrihDocument,Article);
        RashQuantity = AddDProp(IntegerClass,RashDocument,Article);
        ArtToGroup = AddDProp(ArticleGroup,Article);
        DocDate = AddDProp(IntegerClass,Document);
        GrAddV = AddDProp(IntegerClass,ArticleGroup);
        ArtAddV = AddDProp(IntegerClass,Article);

        BarCode = AddDProp(IntegerClass,Article);
        BarCode.Property.OutName = "штрих-код";

        LDP AbsQuantity = AddVProp(null,IntegerClass,Document,Article);
        AbsQuantity.Property.OutName = "абст. кол-во";

        LDP IsGrmat = AddVProp(0,IntegerClass,Article);
        IsGrmat.Property.OutName = "признак товара";

        FilledProperty = AddLProp(0,1,1,IsGrmat,1,1,ArtToGroup,1);
        FilledProperty.Property.OutName = "заполнение гр. тов.";

        // сделаем Quantity перегрузкой
        Quantity = AddLProp(2,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);

        LDP RashValue = AddVProp(-1,IntegerClass,RashDocument);
        RashValue.Property.OutName = "призн. расхода";

        LDP PrihValue = AddVProp(1,IntegerClass,PrihDocument);
        PrihValue.Property.OutName = "призн. прихода";

        OpValue = AddLProp(2,1,1,RashValue,1,1,PrihValue,1);
        OpValue.Property.OutName = "общ. призн.";
        
        LGP RaznSValue = AddGProp(OpValue,true,DocStore,1);
        RaznSValue.Property.OutName = "разн. пр-рас.";
        
        LRP RGrAddV = AddRProp(GrAddV,1,ArtToGroup,1);
        RGrAddV.Property.OutName = "наценка по товару (гр.)";

        LRP ArtActAddV = AddLProp(2,1,1,RGrAddV,1,1,ArtAddV,1);
        ArtActAddV.Property.OutName = "наценка по товару";

//        LRP Quantity = AddLProp(2,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
//
        LSFP Dirihle = AddSFProp("prm1<prm2",true,2);
        LMFP Multiply = AddMFProp(2);

        Name.Property.OutName = "имя";
        DocStore.Property.OutName = "склад";
        Quantity.Property.OutName = "кол-во";
        PrihQuantity.Property.OutName = "кол-во прих.";
        RashQuantity.Property.OutName = "кол-во расх.";
        ArtToGroup.Property.OutName = "гр. тов";
        DocDate.Property.OutName = "дата док.";
        GrAddV.Property.OutName = "нац. по гр.";
        ArtAddV.Property.OutName = "нац. перегр.";

        LRP StoreName = AddRProp(Name,1,DocStore,1);
        StoreName.Property.OutName = "имя склада";
        
        LRP ArtGroupName = AddRProp(Name,1,ArtToGroup,1);
        ArtGroupName.Property.OutName = "имя гр. тов.";

        LDP ArtGName = AddDProp(StringClass,Article);
        ArtGName.Property.OutName = "при доб. гр. тов.";
        SetDefProp(ArtGName,ArtGroupName,true);

        LRP DDep = AddRProp(Dirihle,2,DocDate,1,DocDate,2);
        DDep.Property.OutName = "предш. док.";

        LRP QDep = AddRProp(Multiply,3,DDep,1,2,Quantity,1,3);
        QDep.Property.OutName = "изм. баланса";

        GSum = AddGProp(QDep,true,2,3);
        GSum.Property.OutName = "остаток до операции";

        GP = AddGProp(Quantity,true,DocStore,1,2);
        GP.Property.OutName = "сумм кол-во док. тов.";
        GAP = AddGProp(GP,true,2);
        GAP.Property.OutName = "сумм кол-во тов.";
        G2P = AddGProp(Quantity,true,DocStore,1,ArtToGroup,2);
        G2P.Property.OutName = "скл-гр. тов";

        LGP PrihArtStore = AddGProp(PrihQuantity,true,DocStore,1,2);
        PrihArtStore.Property.OutName = "приход по складу";

        LGP RashArtStore = AddGProp(RashQuantity,true,DocStore,1,2);
        RashArtStore.Property.OutName = "расход по складу";

        OstArtStore = AddLProp(1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);
        OstArtStore.Property.OutName = "остаток по складу";

        OstArt = AddGProp(OstArtStore,true,2);
        OstArt.Property.OutName = "остаток по товару";

        MaxPrih = AddGProp(PrihQuantity,false,DocStore,1,ArtToGroup,2);
        MaxPrih.Property.OutName = "макс. приход по гр. тов.";

        MaxOpStore = AddLProp(0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);
        MaxOpStore.Property.OutName = "макс. операция";
        
        SumMaxArt = AddGProp(MaxOpStore,true,2);
        SumMaxArt.Property.OutName = "сумма макс. операция";
    }

    void InitConstraints() {
        
        Constraints.put((ObjectProperty)OstArtStore.Property,new PositiveConstraint());
        Constraints.put((ObjectProperty)FilledProperty.Property,new NotEmptyConstraint());
        Constraints.put((ObjectProperty)BarCode.Property,new UniqueConstraint());
    }

    void InitPersistents() {

        Persistents.add((AggregateProperty)GP.Property);
        Persistents.add((AggregateProperty)GAP.Property);
        Persistents.add((AggregateProperty)G2P.Property);
        Persistents.add((AggregateProperty)GSum.Property);
        Persistents.add((AggregateProperty)OstArtStore.Property);
        Persistents.add((AggregateProperty)OstArt.Property);
        Persistents.add((AggregateProperty)MaxPrih.Property);
        Persistents.add((AggregateProperty)MaxOpStore.Property);
        Persistents.add((AggregateProperty)SumMaxArt.Property);
        Persistents.add((AggregateProperty)OpValue.Property);
    }

    void InitTables() {
        TableImplement Include;
        
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Store));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ArticleGroup));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        Include.add(new DataPropertyInterface(Document));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        Include.add(new DataPropertyInterface(Store));
        TableFactory.IncludeIntoGraph(Include);
    }

    void InitIndexes() {
    }

    void InitNavigators() {
        BaseGroup.AddChild(new TestNavigatorForm());
    }
}

class TestNavigatorForm extends NavigatorForm<TestBusinessLogics> {
    
    RemoteForm<TestBusinessLogics> CreateForm(DataAdapter Adapter, TestBusinessLogics BL) throws SQLException {
        return new TestRemoteForm(Adapter,BL);
    }
    
}


class TestRemoteForm extends RemoteForm<TestBusinessLogics> {

    // в EJB кто-то должен сказать где брать Adapter, а где BusinessLogics
    TestRemoteForm(DataAdapter Adapter, TestBusinessLogics BL) throws SQLException {
        super(Adapter,BL);

        ObjectImplement obj1 = new ObjectImplement();
        ObjectImplement obj2 = new ObjectImplement();
        ObjectImplement obj3 = new ObjectImplement();
        
        GroupObjectImplement gv = new GroupObjectImplement();
        GroupObjectImplement gv2 = new GroupObjectImplement();
        GroupObjectImplement gv3 = new GroupObjectImplement();

        gv.add(obj1);
        gv2.add(obj2);
        gv3.add(obj3);
        AddGroup(gv);
        AddGroup(gv2);
        AddGroup(gv3);
        gv.GID = 1;
        gv2.GID = 2;
        gv3.GID = 3;

        PropertyObjectImplement GrTovImpl=null;
        PropertyObjectImplement NameImpl=null;
        PropertyObjectImplement DateImpl=null;
        
        Iterator<Property> ipr = BL.Properties.iterator();
        while(ipr.hasNext()) {
            Property DrawProp = ipr.next();
            if(DrawProp.Interfaces.size() == 1 && DrawProp instanceof ObjectProperty) {
                PropertyObjectImplement PropImpl = new PropertyObjectImplement((ObjectProperty)DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj1);
                Properties.add(new PropertyView(PropImpl,gv));
                
                PropImpl = new PropertyObjectImplement((ObjectProperty)DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj2);
                if(DrawProp.OutName.equals("гр. тов"))
                    GrTovImpl = PropImpl;
                Properties.add(new PropertyView(PropImpl,gv2));

                PropImpl = new PropertyObjectImplement((ObjectProperty)DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj3);
                if(DrawProp.OutName.equals("имя"))
                    NameImpl = PropImpl;
                if(DrawProp.OutName.equals("дата док."))
                    DateImpl = PropImpl;
                Properties.add(new PropertyView(PropImpl,gv3));
            }
        }
        
        PropertyObjectImplement QImpl = BL.AddPropView(this,BL.Quantity,gv3,obj3,obj2);
        BL.AddPropView(this,BL.GP,gv3,obj3,obj2);

        BL.AddPropView(this,BL.PrihQuantity,gv3,obj3,obj2);
        BL.AddPropView(this,BL.RashQuantity,gv3,obj3,obj2);
        BL.AddPropView(this,BL.GSum,gv3,obj3,obj2);
        
        GroupObjectValue ChangeValue;

        obj1.OutName = "";
        ChangeGridClass(obj1,BL.ArticleGroup.ID);
        obj2.OutName = "";
        ChangeGridClass(obj2,BL.Article.ID);
        obj3.OutName = "";
        ChangeGridClass(obj3,BL.Document.ID);
//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(NameImpl,"ПРОДУКТЫ");

        AddFilter(new NotNullFilter(QImpl));
        AddFilter(new CompareFilter(GrTovImpl,0,new ObjectValueLink(obj1)));
        
//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(NameImpl,"ПРОДУКТЫ");

//        fbv.AddOrder(NameImpl);
//        fbv.AddOrder(DateImpl);

    }


    @Override
    String GetReportDesign() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    Set<ObjectImplement> GetReportObjects() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    ClientFormBean GetRichDesign() {
        return new ClientFormBean(this);
    }
}
class LP {
    LP(Property iProperty) {
        Property=iProperty;
        ListInterfaces = new ArrayList<PropertyInterface>();
    }
    Property Property;
    List<PropertyInterface> ListInterfaces;
}

class LDP extends LP {

    LDP(Property iProperty) {super(iProperty);}

    void AddInterface(Class InClass) {
        DataPropertyInterface Interface = new DataPropertyInterface(InClass);
        ListInterfaces.add(Interface);
        Property.Interfaces.add(Interface);
    }
    
    void ChangeProperty(ChangesSession Session,DataAdapter Adapter,Object Value,Integer ...iParams) throws SQLException {
        Map<PropertyInterface,ObjectValue> Keys = new HashMap();
        Integer IntNum = 0;
        for(int i : iParams) {
            DataPropertyInterface Interface = (DataPropertyInterface)ListInterfaces.get(IntNum);
            Keys.put(Interface,new ObjectValue(i,Interface.Class));
            IntNum++;
        }
        
        ((DataProperty)Property).ChangeProperty(Adapter, Keys, Value, Session);
    }

}

class LSFP extends LP {

    LSFP(Property iProperty,IntegralClass iClass,int Objects) {
        super(iProperty);
        for(int i=0;i<Objects;i++) {
            StringFormulaPropertyInterface Interface = new StringFormulaPropertyInterface(iClass,"prm"+(i+1));
            ListInterfaces.add(Interface);
            Property.Interfaces.add(Interface);
        }
    }
}

class LMFP extends LP {

    LMFP(Property iProperty,IntegralClass iClass,int Objects) {
        super(iProperty);
        for(int i=0;i<Objects;i++) {
            FormulaPropertyInterface Interface = new FormulaPropertyInterface(iClass);
            ListInterfaces.add(Interface);
            Property.Interfaces.add(Interface);
        }
    }
}


class LRP extends LP {
    
    LRP(Property iProperty,int Objects) {
        super(iProperty);
        for(int i=0;i<Objects;i++) {
            PropertyInterface Interface = new PropertyInterface();
            ListInterfaces.add(Interface);
            Property.Interfaces.add(Interface);
        }
    }
}

class LGP extends LP {
    
    LP GroupProperty;
    LGP(Property iProperty,LP iGroupProperty) {
        super(iProperty);
        GroupProperty = iGroupProperty;
    }
    
    void AddInterface(PropertyInterfaceImplement Implement) {
        GroupPropertyInterface Interface = new GroupPropertyInterface(Implement);
        ListInterfaces.add(Interface);
        Property.Interfaces.add(Interface);
    }
}

/*
    List<PropertyView> GetPropViews(RemoteForm fbv, Property prop) {
        
        List<PropertyView> result = new ArrayList();
        
        for (PropertyView propview : fbv.Properties)
            if (propview.View.Property == prop) result.add(propview);
       
        return result;        
    }
    
    // "СЂРёСЃСѓРµС‚" РєР»Р°СЃСЃ, СЃРѕ РІСЃРµРјРё СЃРІ-РІР°РјРё
    void DisplayClasses(DataAdapter Adapter, DataPropertyInterface[] ToDraw) throws SQLException {

        Map<DataPropertyInterface,SourceExpr> JoinSources = new HashMap<DataPropertyInterface,SourceExpr>();
        SelectQuery SimpleSelect = new SelectQuery(null);
        FromTable PrevSelect = null;
        for(int ic=0;ic<ToDraw.length;ic++) {
            FromTable Select = TableFactory.ObjectTable.ClassSelect(ToDraw[ic].Class);
            Select.JoinType = "FULL";
            if(PrevSelect==null) 
                SimpleSelect.From = Select;
            else
                PrevSelect.Joins.add(Select);
            
            PrevSelect = Select;
            JoinSources.put(ToDraw[ic],new FieldSourceExpr(Select,TableFactory.ObjectTable.Key.Name));
        }
        
        JoinList Joins=new JoinList();
        
        Integer SelFields = 0;
        Iterator<Property> i = Properties.iterator();
        while(i.hasNext()) {
            Property Prop = i.next();
            
            MapBuilder<PropertyInterface,DataPropertyInterface> mb= new MapBuilder<PropertyInterface,DataPropertyInterface>();
            List<Map<PropertyInterface,DataPropertyInterface>> Maps = mb.BuildMap((PropertyInterface[])Prop.Interfaces.toArray(new PropertyInterface[0]), ToDraw);
            // РїРѕРїСЂРѕР±СѓРµРј РІСЃРµ РІР°СЂРёР°РЅС‚С‹ РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ
            Iterator<Map<PropertyInterface,DataPropertyInterface>> im = Maps.iterator();
            while(im.hasNext()) {
                Map<PropertyInterface,DataPropertyInterface> Impl = im.next();
                Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                
                InterfaceClass ClassImplement = new InterfaceClass();
                Iterator<PropertyInterface> ip = Prop.Interfaces.iterator();
                while(ip.hasNext()) {
                    PropertyInterface Interface = ip.next();
                    DataPropertyInterface MapInterface = Impl.get(Interface);
                    ClassImplement.put(Interface,MapInterface.Class);
                    JoinImplement.put(Interface,JoinSources.get(MapInterface));
                }
                
                if(Prop.GetValueClass(ClassImplement)!=null) {
                    // С‚Рѕ РµСЃС‚СЊ Р°РєС‚СѓР°Р»СЊРЅРѕРµ СЃРІ-РІРѕ
                    SimpleSelect.Expressions.put("test"+(SelFields++).toString(),Prop.JoinSelect(Joins,JoinImplement,false));
                }
            }
        }
        
        Iterator<From> ij = Joins.iterator();
        while(ij.hasNext()) {
            From Join = ij.next();
            Join.JoinType = "LEFT";
            PrevSelect.Joins.add(Join);
        }

        Adapter.OutSelect(SimpleSelect);
    }
*/