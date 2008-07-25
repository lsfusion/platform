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
        
        show(new ClientForm(this));
    }
}
/**
 *
 * @author ME
 */
class Test extends BusinessLogics  {
    
    void SimpleTest(ClientForm Form) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        
        Class Base = new ObjectClass(3);
        Base.AddParent(BaseClass);
        Class Article = new ObjectClass(4);
        Article.AddParent(Base);
        Class Store = new ObjectClass(5);
        Store.AddParent(Base);
        Class Document = new ObjectClass(6);
        Document.AddParent(Base);
        Class PrihDocument = new ObjectClass(7);
        PrihDocument.AddParent(Document);
        Class RashDocument = new ObjectClass(8);
        RashDocument.AddParent(Document);
        Class ArticleGroup = new ObjectClass(9);
        ArticleGroup.AddParent(Base);
        
        LDP Name = AddDProp(StringClass,Base);
        LDP DocStore = AddDProp(Store,Document);
        LDP PrihQuantity = AddDProp(IntegerClass,PrihDocument,Article);
        LDP RashQuantity = AddDProp(IntegerClass,RashDocument,Article);
        LDP ArtToGroup = AddDProp(ArticleGroup,Article);
        LDP DocDate = AddDProp(IntegerClass,Document);
        LDP GrAddV = AddDProp(IntegerClass,ArticleGroup);
        LDP ArtAddV = AddDProp(IntegerClass,Article);

        LDP BarCode = AddDProp(IntegerClass,Article);
        BarCode.Property.OutName = "штрих-код";

        LDP AbsQuantity = AddVProp(null,IntegerClass,Document,Article);
        AbsQuantity.Property.OutName = "абст. кол-во";

        LDP IsGrmat = AddVProp(0,IntegerClass,Article);
        IsGrmat.Property.OutName = "признак товара";

        LRP FilledProperty = AddLProp(0,1,1,IsGrmat,1,1,ArtToGroup,1);
        FilledProperty.Property.OutName = "заполнение гр. тов.";

        // сделаем Quantity перегрузкой
        LRP Quantity = AddLProp(2,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);

        LDP RashValue = AddVProp(-1,IntegerClass,RashDocument);
        RashValue.Property.OutName = "призн. расхода";

        LDP PrihValue = AddVProp(1,IntegerClass,PrihDocument);
        PrihValue.Property.OutName = "призн. прихода";

        LRP OpValue = AddLProp(2,1,1,RashValue,1,1,PrihValue,1);
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

        LRP DDep = AddRProp(Dirihle,2,DocDate,1,DocDate,2);
        DDep.Property.OutName = "предш. док.";

        LRP QDep = AddRProp(Multiply,3,DDep,1,2,Quantity,1,3);
        QDep.Property.OutName = "изм. баланса";

        LGP GSum = AddGProp(QDep,true,2,3);
        GSum.Property.OutName = "остаток до операции";

        LGP GP = AddGProp(Quantity,true,DocStore,1,2);
        GP.Property.OutName = "сумм кол-во док. тов.";
        LGP GAP = AddGProp(GP,true,2);
        GAP.Property.OutName = "сумм кол-во тов.";
        LGP G2P = AddGProp(Quantity,true,DocStore,1,ArtToGroup,2);
        G2P.Property.OutName = "скл-гр. тов";

        LGP PrihArtStore = AddGProp(PrihQuantity,true,DocStore,1,2);
        PrihArtStore.Property.OutName = "приход по складу";

        LGP RashArtStore = AddGProp(RashQuantity,true,DocStore,1,2);
        RashArtStore.Property.OutName = "расход по складу";

        LRP OstArtStore = AddLProp(1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);
        OstArtStore.Property.OutName = "остаток по складу";

        LGP OstArt = AddGProp(OstArtStore,true,2);
        OstArt.Property.OutName = "остаток по товару";

        LGP MaxPrih = AddGProp(PrihQuantity,false,DocStore,1,ArtToGroup,2);
        MaxPrih.Property.OutName = "макс. приход по гр. тов.";

        LRP MaxOpStore = AddLProp(0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);
        MaxOpStore.Property.OutName = "макс. операция";
        
        LGP SumMaxArt = AddGProp(MaxOpStore,true,2);
        SumMaxArt.Property.OutName = "сумма макс. операция";

        TableImplement Include;
        
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Base));
        TableFactory.IncludeIntoGraph(Include);
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
        Include.add(new DataPropertyInterface(Base));
        Include.add(new DataPropertyInterface(Base));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        Include.add(new DataPropertyInterface(Document));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        Include.add(new DataPropertyInterface(Store));
        TableFactory.IncludeIntoGraph(Include);
        
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
        
        Constraints.put((ObjectProperty)OstArtStore.Property,new PositiveConstraint());
        Constraints.put((ObjectProperty)FilledProperty.Property,new NotEmptyConstraint());
        Constraints.put((ObjectProperty)BarCode.Property,new UniqueConstraint());
        
        DataAdapter ad = new DataAdapter();
        ad.Connect("");

        FillDB(ad);
        
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

/*        Quantity.ChangeProperty(Session,ad,8,PrihDocuments[1],Articles[4]);
        Quantity.ChangeProperty(Session,ad,10,PrihDocuments[1],Articles[2]);
        Quantity.ChangeProperty(Session,ad,20,PrihDocuments[1],Articles[3]);
        Quantity.ChangeProperty(Session,ad,28,PrihDocuments[1],Articles[4]);
        Quantity.ChangeProperty(Session,ad,7,PrihDocuments[2],Articles[1]);
        Quantity.ChangeProperty(Session,ad,40,PrihDocuments[2],Articles[2]);
        Quantity.ChangeProperty(Session,ad,50,PrihDocuments[2],Articles[3]);
        Quantity.ChangeProperty(Session,ad,68,PrihDocuments[2],Articles[4]);
*/
//        List<AggregateProperty> UpdateProps = new ArrayList();
//        UpdateProps.add((AggregateProperty)QDep.Property);
//        UpdateAggregations(ad, UpdateProps, Session);

//        ((ObjectProperty)QDep.Property).OutChangesTable(ad, Session);

        Apply(ad,Session);
        
//       OpValue.Property.Out(ad);
//        if(true) return;
//        Quantity.Property.Out(ad);

//        OstArtStore.Property.Out(ad);
//        OstArt.Property.Out(ad);
//        ((ObjectProperty)MaxPrih.Property).OutChangesTable(ad, Session);
//        MaxPrih.Property.Out(ad);
//        PrihArtStore.Property.Out(ad);
//        RashArtStore.Property.Out(ad);
//        MaxOpStore.Property.Out(ad);

//        UpdateProps.add((AggregateProperty)GAP.Property);
//        UpdateAggregations(ad, UpdateProps, Session);

//        ((ObjectProperty)G2P.Property).OutChangesTable(ad, Session);

//        CheckPersistent(ad);        
        
//        Name.Property.Out(ad);
//        GP.Property.Out(ad);
//        QDep.Property.Out(ad);

//        Session = new ChangesSession(1);
/*        Name.ChangeProperty(Session,ad,"PRODUCTS",ArticleGroups[0]);
        Name.ChangeProperty(Session,ad,"CLOTHES",ArticleGroups[1]);

        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[4]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[5]);

        ((ObjectProperty)Name.Property).OutChangesTable(ad, Session);
        
        List<AggregateProperty> UpdateProps = new ArrayList();
        UpdateProps.add((AggregateProperty)ArtGroupName.Property);
        UpdateAggregations(ad, UpdateProps, Session);
        
        ((ObjectProperty)ArtGroupName.Property).OutChangesTable(ad, Session);
 */      
        
//        Quantity.ChangeProperty(Session,ad,1,Documents[0],Articles[0]);
//        Quantity.ChangeProperty(Session,ad,4,Documents[1],Articles[0]);
//        Quantity.ChangeProperty(Session,ad,4,Documents[2],Articles[3]);

//        PrihQuantity.ChangeProperty(Session,ad,2,PrihDocuments[3],Articles[1]);
//        PrihQuantity.ChangeProperty(Session,ad,23,PrihDocuments[3],Articles[4]);
//        PrihQuantity.ChangeProperty(Session,ad,1,PrihDocuments[1],Articles[0]);
//        RashQuantity.ChangeProperty(Session,ad,111,RashDocuments[1],Articles[5]);
//        RashQuantity.ChangeProperty(Session,ad,2222,RashDocuments[4],Articles[3]);

//        DocDate.ChangeProperty(Session,ad,1002,PrihDocuments[2]);
//        DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[5]);
//        Quantity.ChangeProperty(Session,ad,12,PrihDocuments[3],Articles[3]);
//        DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[1]);
//        Quantity.ChangeProperty(Session,ad,3,Documents[0],Articles[3]);
//        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[3]);

//        ((ObjectProperty)DocStore.Property).OutChangesTable(ad, Session);
        
//        List<AggregateProperty> UpdateProps = new ArrayList();
//        UpdateProps.add((AggregateProperty)G2P.Property);
//        UpdateProps.add((AggregateProperty)GP.Property);
//        UpdateProps.add((AggregateProperty)GAP.Property);
//        UpdateAggregations(ad, UpdateProps, Session);

//        ((ObjectProperty)GP.Property).OutChangesTable(ad, Session);
        
//        Apply(ad,Session);

//        MaxPrih.Property.Out(ad);

//        OstArt.Property.Out(ad);
        
//        MaxOpStore.Property.Out(ad);
//        GSum.Property.Out(ad);
//        GP.Property.Out(ad);
//        G2P.Property.Out(ad);
//        CheckPersistent(ad);
        
//        ChangeDBTest(ad);

//        if(true) return;
        // РїРѕС‚РµСЃС‚РёРј FormBeanView
        // РїРѕРєР° РѕРґРёРЅ РІРёРґ РїРѕС‚РµСЃС‚РёРј
        FormBeanView fbv  = new FormBeanView(ad,this);
        ObjectImplement obj1 = new ObjectImplement();                
        ObjectImplement obj2 = new ObjectImplement();
        ObjectImplement obj3 = new ObjectImplement();
        
        GroupObjectImplement gv = new GroupObjectImplement();
        GroupObjectImplement gv2 = new GroupObjectImplement();
        GroupObjectImplement gv3 = new GroupObjectImplement();

        gv.add(obj1);
        gv2.add(obj2);
        gv3.add(obj3);
        fbv.AddGroup(gv);
        fbv.AddGroup(gv2);
        fbv.AddGroup(gv3);
        gv.GID = 1;
        gv2.GID = 2;
        gv2.GID = 3;

        PropertyObjectImplement GrTovImpl=null;
        PropertyObjectImplement NameImpl=null;
        PropertyObjectImplement DateImpl=null;
        
        Iterator<Property> ipr = Properties.iterator();
        while(ipr.hasNext()) {
            Property DrawProp = ipr.next();
            if(DrawProp.Interfaces.size() == 1 && DrawProp instanceof ObjectProperty) {
                PropertyObjectImplement PropImpl = new PropertyObjectImplement((ObjectProperty)DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj1);
                if(DrawProp.OutName.equals("имя"))
                    NameImpl = PropImpl;
                fbv.Properties.add(new PropertyView(PropImpl,gv));
                
                PropImpl = new PropertyObjectImplement((ObjectProperty)DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj2);
                if(DrawProp.OutName.equals("гр. тов"))
                    GrTovImpl = PropImpl;
                if(DrawProp.OutName.equals("дата док."))
                    DateImpl = PropImpl;
                fbv.Properties.add(new PropertyView(PropImpl,gv2));

                PropImpl = new PropertyObjectImplement((ObjectProperty)DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj3);
                fbv.Properties.add(new PropertyView(PropImpl,gv3));
            }
        }
        
        PropertyObjectImplement QImpl = AddPropView(fbv,Quantity,gv2,obj2,obj1);
        AddPropView(fbv,GP,gv2,obj2,obj1);

        AddPropView(fbv,PrihQuantity,gv2,obj2,obj1);
        AddPropView(fbv,RashQuantity,gv2,obj2,obj1);
        AddPropView(fbv,GSum,gv2,obj2,obj1);
        
        GroupObjectValue ChangeValue;

        obj1.OutName = "";
        fbv.ChangeGridClass(obj1,Article.ID);
        obj2.OutName = "";
        fbv.ChangeGridClass(obj2,Document.ID);
        obj3.OutName = "";
        fbv.ChangeGridClass(obj3,ArticleGroup.ID);
//        fbv.AddFilter(new NotNullFilter(QImpl));
        fbv.AddOrder(NameImpl);
        fbv.AddOrder(DateImpl);
        
        
//        fbv.ChangeProperty(QImpl, 10);
//        fbv.EndApply().Out(fbv);

        FormChanges fc;
        
        fc = fbv.EndApply();
        fc.Out(fbv);
        
        ClientFormBean cfc = new ClientFormBean(fbv);
        Form.clientBean = cfc;
        
        List<PropertyView> ps;
        
        ps = GetPropViews(fbv, Name.Property);
//        for (PropertyView p : ps) cfc.client(p).maxWidth = 50;
        
        
        Form.initializeForm();
        
        Form.applyFormChanges(cfc.convertFormChangesToClient(fc)); 

//        ad.Disconnect();
    }
    
    PropertyObjectImplement AddPropView(FormBeanView fbv,LP ListProp,GroupObjectImplement gv,ObjectImplement... Params) {
        PropertyObjectImplement PropImpl = new PropertyObjectImplement((ObjectProperty)ListProp.Property);
        
        ListIterator<PropertyInterface> i = ListProp.ListInterfaces.listIterator();
        for(ObjectImplement Object : Params) {
            PropImpl.Mapping.put(i.next(),Object);
        }
        fbv.Properties.add(new PropertyView(PropImpl,gv));
        return PropImpl;
    }
    
    List<PropertyView> GetPropViews(FormBeanView fbv, Property prop) {
        
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
}

// РґР»СЏ СѓРїСЂРѕС‰РµРЅРЅРѕРіРѕ СЃРѕР·РґР°РЅРёСЏ СЃРІ-РІ СЃРѕ СЃРїРёСЃРєР°РјРё РёРЅС‚РµСЂС„РµР№СЃРѕРІ, РїРѕ СЃСѓС‚Рё РєР°Рє С„Р°СЃР°Рґ

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
