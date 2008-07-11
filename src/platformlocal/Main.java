/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

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

public class Main  {
    
    Class[] ClassList;

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
  
        System.out.print(1<<0);
//          java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver"); 
//        java.lang.Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
//          Connection cn = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");
//        Connection cn = DriverManager.getConnection("jdbc:sqlserver://server:1433;User=sa;Password=");

        Test t = new Test();
        t.SimpleTest();
    }
}

/**
 *
 * @author ME
 */
class Test extends BusinessLogics  {
    
    Class[] ClassList;

    public void Run() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        // TODO code application logic here
        
        ClassList = new Class[9];
        for(int i=0;i<9;i++) ClassList[i] = new Class(i);
        ClassList[1].AddParent(ClassList[0]);
        ClassList[2].AddParent(ClassList[0]);
        ClassList[3].AddParent(ClassList[1]);
        ClassList[3].AddParent(ClassList[2]);
        ClassList[4].AddParent(ClassList[2]);
        ClassList[4].AddParent(ClassList[3]);
        ClassList[5].AddParent(ClassList[2]);
        ClassList[6].AddParent(ClassList[5]);
        ClassList[6].AddParent(ClassList[4]);
        ClassList[7].AddParent(ClassList[4]);
        ClassList[8].AddParent(ClassList[3]);

        // TESY - InterfaceClassSet, InterfaceClass 
/*
        List<PropertyInterface> ti = new ArrayList<PropertyInterface>();
        PropertyInterface a = new DataPropertyInterface(null);
        PropertyInterface b = new DataPropertyInterface(null);
        PropertyInterface c = new DataPropertyInterface(null);
        ti.add(a);
        ti.add(b);
        ti.add(c);
        
        InterfaceClass opInt,opIntA;
        InterfaceClassSet opA = new InterfaceClassSet();
        opIntA = new InterfaceClass();
        opIntA.put(a,ClassList[5]);
        opIntA.put(b,ClassList[1]);
        opA.add(opIntA);

        opInt = new InterfaceClass();
        opInt.put(a,ClassList[7]);
        opInt.put(b,ClassList[2]);
        opA.add(opInt);

        InterfaceClassSet opB = new InterfaceClassSet();
        InterfaceClass opIntB = new InterfaceClass();
        opIntB.put(a,ClassList[8]);
        opIntB.put(b,ClassList[2]);
        opIntB.put(c,ClassList[6]);
        opB.add(opIntB);

        opInt = new InterfaceClass();
        opInt.put(a,ClassList[3]);
        opInt.put(b,ClassList[2]);
        opInt.put(c,ClassList[7]);
        opB.add(opInt);

        opInt = new InterfaceClass();
        opInt.put(a,ClassList[7]);
        opInt.put(b,ClassList[2]);
//        opB.add(opInt);

//        opIntA.And(opIntB).Out(ti);
       opA.AndSet(opB).Out(ti);
//        opA.Out(ti);

*/
        // TEST - TableImplement
        TableImplement Include;
        TableFactory Factory = TableFactory;
        
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[0]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----0-----");
        
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[1]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----1-----");
        
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[8]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----8-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[4]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----4-----");
        
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[2]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----2-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[6]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----6-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[0]));
        Include.add(new DataPropertyInterface(ClassList[0]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----0 7-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[0]));
        Include.add(new DataPropertyInterface(ClassList[7]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----0 7-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[0]));
        Include.add(new DataPropertyInterface(ClassList[8]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----0 0-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[5]));
        Include.add(new DataPropertyInterface(ClassList[3]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----4 3-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[1]));
        Include.add(new DataPropertyInterface(ClassList[2]));
        Factory.IncludeIntoGraph(Include);

        Factory.Out();
        System.out.println("----1 2-----");

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ClassList[8]));
        Include.add(new DataPropertyInterface(ClassList[4]));
        Factory.IncludeIntoGraph(Include);
        
        Factory.Out();
        System.out.println("----8 4-----");

        // TEST - Data Properties

        LP[] Props = new LP[20];
        
        AddExDProp(3,0);
        Props[0] = AddExDProp(4,0);
        Props[9] = AddExDProp(5,0);
//        Props[1].ListInterfaces.get(0).ValueClass = ClassList[4];
//        System.out.println(Props[1].Property.GetValueClass().ID);
        Props[11] = AddExDProp(6,3);
        Props[3] = AddExDProp(4,3);
        Props[8] = AddExDProp(5,2);
        Props[1] = AddExDProp(8,4);
        AddExDProp(4,2,8);
        Props[4] = AddExDProp(3,5,1);
        AddExDProp(7,6,1);
        Props[5] = AddExDProp(1,6,3);
        
        Props[2] = AddRProp(Props[4],2,1,Props[5],2,1);
//        Props[2].ListInterfaces.get(0).ValueClass = ClassList[6];
//        Props[2].ListInterfaces.get(1).ValueClass = ClassList[6];
//        System.out.println(Props[2].Property.GetValueClass().ID);
//        Props[2].Property.GetClassSet(null).Out(Props[2].Property.Interfaces);
        Props[10] = AddRProp(Props[3],2,Props[2],1,2);
        Props[10].Property.GetClassSet(null).Out(Props[10].Property.Interfaces);

        Props[6] = AddRProp(Props[2],1,1,1);
//        Props[6].ListInterfaces.get(0).ValueClass = ClassList[6];
//        System.out.println(Props[6].Property.GetValueClass().ID);
//        Props[6].Property.GetClassSet(null).Out(Props[6].Property.Interfaces);

        Props[7] = AddGProp(Props[4],true,Props[8],1);
        RegGClass((LGP)Props[7],1,2);
//        Props[7].ListInterfaces.get(0).ValueClass = ClassList[6];
//        System.out.println(Props[7].Property.GetValueClass().ID);
//        Props[7].Property.GetClassSet(null).Out(Props[7].Property.Interfaces);
        
        // TEST - Adapter

        DataAdapter ad = new DataAdapter();
 //       try {
            ad.Connect("");
 //       } catch(Exception e) {
            
//        }
        
        FillDB(ad);

        // TEST - Other Properties
        // RelationalProperties

        // TEST - JOIN SELECT'ов
        // пока будем на dumbe тестить
        SelectQuery SimpleSelect = new SelectQuery(new FromTable("dumb"));
        // Props[0] проверим
        // для этого сначала в JoinImplement закинем
        SourceExpr SourceJoin = new ValueSourceExpr(1);
        SourceExpr SourceJoin2 = new ValueSourceExpr(2);

        JoinList Joins=new JoinList();
        
/*        Props[7].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.put("test1",Props[7].Property.JoinSelect(Joins));
        Props[0].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.put("test1",Props[0].Property.JoinSelect(Joins));
        Props[9].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.put("test2",Props[9].Property.JoinSelect(Joins));
        Props[2].ListInterfaces.get(0).JoinImplement = SourceJoin;
        Props[2].ListInterfaces.get(1).JoinImplement = SourceJoin2;
        SimpleSelect.Expressions.put("test3",Props[2].Property.JoinSelect(Joins));
        Props[4].ListInterfaces.get(0).JoinImplement = SourceJoin;
        Props[4].ListInterfaces.get(1).JoinImplement = SourceJoin2;
        SimpleSelect.Expressions.put("test4",Props[4].Property.JoinSelect(Joins));
        Props[6].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.put("test5",Props[6].Property.JoinSelect(Joins));
        Props[10].ListInterfaces.get(0).JoinImplement = SourceJoin;
        Props[10].ListInterfaces.get(1).JoinImplement = SourceJoin2;
        SimpleSelect.Expressions.put("test6",Props[10].Property.JoinSelect(Joins));
*/
        Iterator<From> ij = Joins.iterator();
        while(ij.hasNext()) SimpleSelect.From.Joins.add(ij.next());
        ad.OutSelect(SimpleSelect);

        ad.Disconnect();

        
        // TEST - поиска имплементации
                /*
        List<DataPropertyInterface> FindItem = new ArrayList<DataPropertyInterface>();
        FindItem.add(new DataPropertyInterface(ClassList[2]));
        TableImplement FindImpl = Factory.GetTable(FindItem, null);
        FindImpl.OutClasses();*/

/*      
        Factory.FillDB(); 
 
        Map<KeyField,DataPropertyInterface> MapKF = new HashMap<KeyField,DataPropertyInterface>();
        List<DataPropertyInterface> FindItem = new ArrayList<DataPropertyInterface>();
        FindItem.add(new DataPropertyInterface(ClassList[1]));
        FindItem.add(new DataPropertyInterface(ClassList[8]));
        TableImplement FindImpl = Factory.GetTable(FindItem,MapKF);
        FindImpl.OutClasses();
        
        Iterator<KeyField> it = FindImpl.Table.KeyFields.iterator();
        while(it.hasNext()) {
            KeyField Field = it.next();
            System.out.println(Field.Name+" "+MapKF.get(Field).Class.ID);
        }

        Iterator<DataPropertyInterface> i = FindImpl.iterator();
        while(i.hasNext()) {
            DataPropertyInterface Interface = i.next();
            System.out.println(Interface.Class.ID.toString()+" "+FindImpl.MapFields.get(Interface).Name);
        }
*/
        
    }

    LDP AddExDProp(int iValue,int ...iParams) {
        DataProperty Property = new DataProperty(TableFactory,ClassList[iValue]);
        LDP ListProperty = new LDP(Property);
        for(int iInt : iParams) {
            ListProperty.AddInterface(ClassList[iInt]);
        }
        AddDataProperty(Property);
        return ListProperty;
    }

    void RegExGClass(LGP GroupProp,int ...iParams) {
        int iInt=0;
        boolean bInt=true;
        for(int i : iParams) {
            if(bInt) {
                iInt = (Integer)i-1;
                bInt = false;
            } else {
                ((GroupProperty)GroupProp.Property).ToClasses.put(GroupProp.GroupProperty.ListInterfaces.get(iInt),ClassList[i]);
                bInt = true;
            }
        }        
    }

    LDP AddDProp(Class Value,Class ...Params) {
        DataProperty Property = new DataProperty(TableFactory,Value);
        LDP ListProperty = new LDP(Property);
        for(Class Int : Params) {
            ListProperty.AddInterface(Int);
        }
        AddDataProperty(Property);
        return ListProperty;
    }

    LSFP AddSFProp(String Formula,Integer Params) {
        StringFormulaProperty Property = new StringFormulaProperty(Formula);
        LSFP ListProperty = new LSFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    
    List<PropertyInterfaceImplement> ReadPropImpl(LP MainProp,Object ...Params) {
        List<PropertyInterfaceImplement> Result = new ArrayList<PropertyInterfaceImplement>();
        int WaitInterfaces = 0, MainInt = 0;
        PropertyMapImplement MapRead = null;
        LP PropRead = null;
        for(Object P : Params) {
            if(P instanceof Integer) {
                // число может быть как ссылкой на родной интерфейс так и 
                PropertyInterface PropInt = MainProp.ListInterfaces.get((Integer)P-1);
                if(WaitInterfaces==0) {
                    // родную берем 
                    Result.add(PropInt);
                } else {
                    // докидываем в маппинг
                    MapRead.Mapping.put(PropRead.ListInterfaces.get(PropRead.ListInterfaces.size()-WaitInterfaces), PropInt);
                    WaitInterfaces--;
                }
            } else {
               // имплементация, типа LP
               PropRead = (LP)P;
               MapRead = new PropertyMapImplement(PropRead.Property);
               WaitInterfaces = PropRead.ListInterfaces.size();
               Result.add(MapRead);
            }
        }
        
        return Result;
    }

    LRP AddRProp(LP MainProp, int IntNum, Object ...Params) {
        RelationProperty Property = new RelationProperty(TableFactory,MainProp.Property);
        LRP ListProperty = new LRP(Property,IntNum);
        int MainInt = 0;
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(ListProperty,Params);
        ListIterator<PropertyInterfaceImplement> i = PropImpl.listIterator();
        while(i.hasNext()) {
            Property.Implements.Mapping.put(MainProp.ListInterfaces.get(MainInt), i.next());
            MainInt++;
        }
        Properties.add(Property);

        return ListProperty;
    }
    
    LGP AddGProp(LP GroupProp,boolean Sum,Object ...Params) {
        GroupProperty Property = null;
        if(Sum)
            Property = new SumGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        else
            Property = new MaxGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        LGP ListProperty = new LGP(Property,GroupProp);
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(GroupProp,Params);
        ListIterator<PropertyInterfaceImplement> i = PropImpl.listIterator();
        while(i.hasNext()) ListProperty.AddInterface(i.next());
        Properties.add(Property);
        
        return ListProperty;
    }

    LRP AddLProp(int ListType, int IntNum, Object ...Params) {
        ListProperty Property = null;
        switch(ListType) {
            case 0:
                Property = new MaxListProperty(TableFactory);
                break;
            case 1:
                Property = new SumListProperty(TableFactory);
                break;
            case 2:
                Property = new OverrideListProperty(TableFactory);
                break;
        }
        
        LRP ListProperty = new LRP(Property,IntNum);

        for(int i=0;i<IntNum;i++) {
            Integer Offs = i*(IntNum+2);
            LP OpImplement = (LP)Params[Offs+1];
            PropertyMapImplement Operand = new PropertyMapImplement(OpImplement.Property);
            for(int j=0;j<IntNum;j++)
                Operand.Mapping.put(OpImplement.ListInterfaces.get(((Integer)Params[Offs+2+j])-1),ListProperty.ListInterfaces.get(j));
            Property.Operands.add(Operand);
            Property.Coeffs.put(Operand,(Integer)Params[Offs]);
        }
        Properties.add(Property);

        return ListProperty;
    }

    void RegGClass(LGP GroupProp,Object ...iParams) {
        int iInt=0;
        boolean bInt=true;
        for(Object i : iParams) {
            if(bInt) {
                iInt = (Integer)i-1;
                bInt = false;
            } else {
                ((GroupProperty)GroupProp.Property).ToClasses.put(GroupProp.GroupProperty.ListInterfaces.get(iInt),(Class)i);
                bInt = true;
            }
        }        
    }
    
    
    void SimpleTest() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        
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
        LDP Quantity = AddDProp(IntegerClass,Document,Article);
        LDP PrihQuantity = AddDProp(IntegerClass,PrihDocument,Article);
        LDP RashQuantity = AddDProp(IntegerClass,RashDocument,Article);
        LDP ArtToGroup = AddDProp(ArticleGroup,Article);
        LDP DocDate = AddDProp(IntegerClass,Document);

        LSFP Dirihle = AddSFProp("CASE WHEN prm1<prm2 THEN prm3 ELSE 0 END",3);

        Name.Property.OutName = "имя";
        DocStore.Property.OutName = "склад";
        Quantity.Property.OutName = "кол-во";
        PrihQuantity.Property.OutName = "кол-во прих.";
        RashQuantity.Property.OutName = "кол-во расх.";
        ArtToGroup.Property.OutName = "гр. тов";
        DocDate.Property.OutName = "дата док.";

        LRP StoreName = AddRProp(Name,1,DocStore,1);
        StoreName.Property.OutName = "имя склада";
        LRP ArtGroupName = AddRProp(Name,1,ArtToGroup,1);
        ArtGroupName.Property.OutName = "имя гр. тов.";

        LRP QDep = AddRProp(Dirihle,3,DocDate,1,DocDate,2,Quantity,1,3);
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
        
        PersistentProperties.add((AggregateProperty)GP.Property);
        PersistentProperties.add((AggregateProperty)GAP.Property);
        PersistentProperties.add((AggregateProperty)G2P.Property);
        PersistentProperties.add((AggregateProperty)GSum.Property);
        PersistentProperties.add((AggregateProperty)OstArtStore.Property);
        PersistentProperties.add((AggregateProperty)OstArt.Property);
        PersistentProperties.add((AggregateProperty)MaxPrih.Property);
        PersistentProperties.add((AggregateProperty)MaxOpStore.Property);
        
        DataAdapter ad = new DataAdapter();
        ad.Connect("");

        FillDB(ad);
        
        ChangesSession Session = new ChangesSession(2);
        
        Integer i;
        Integer[] Articles = new Integer[6];
        for(i=0;i<Articles.length;i++) Articles[i]=Article.AddObject(ad, TableFactory);
        Name.ChangeProperty(Session,ad,"КОЛБАСА", Articles[0]);
        Name.ChangeProperty(Session,ad,"ТВОРОГ", Articles[1]);
        Name.ChangeProperty(Session,ad,"МОЛОКО", Articles[2]);
        Name.ChangeProperty(Session,ad,"ОБУВЬ", Articles[3]);
        Name.ChangeProperty(Session,ad,"ДЖЕМПЕР", Articles[4]);
        Name.ChangeProperty(Session,ad,"МАЙКА", Articles[5]);

        Integer[] Stores = new Integer[2];
        for(i=0;i<Stores.length;i++) Stores[i]=Store.AddObject(ad, TableFactory);
        Name.ChangeProperty(Session,ad,"СКЛАД", Stores[0]);
        Name.ChangeProperty(Session,ad,"ТЗАЛ", Stores[1]);
        
        Integer[] PrihDocuments = new Integer[6];
        for(i=0;i<PrihDocuments.length;i++) {
            PrihDocuments[i]=PrihDocument.AddObject(ad, TableFactory);
            Name.ChangeProperty(Session,ad,"ПР ДОК "+i.toString(), PrihDocuments[i]);
        }

        Integer[] RashDocuments = new Integer[6];
        for(i=0;i<RashDocuments.length;i++) {
            RashDocuments[i]=RashDocument.AddObject(ad, TableFactory);
            Name.ChangeProperty(Session,ad,"РАСХ ДОК "+i.toString(), RashDocuments[i]);
        }

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
        
        Integer[] ArticleGroups = new Integer[2];
        for(i=0;i<ArticleGroups.length;i++) ArticleGroups[i]=ArticleGroup.AddObject(ad, TableFactory);
        Name.ChangeProperty(Session,ad,"ПРОДУКТЫ", ArticleGroups[0]);
        Name.ChangeProperty(Session,ad,"ОДЕЖДА", ArticleGroups[1]);
        
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[0]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[1]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[2]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[3]);
        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[1],Articles[4]);

        // Quantity
        PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[0],Articles[0]);
        RashQuantity.ChangeProperty(Session,ad,5,RashDocuments[0],Articles[0]);
        RashQuantity.ChangeProperty(Session,ad,3,RashDocuments[1],Articles[0]);
        
        PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[0],Articles[1]);
        PrihQuantity.ChangeProperty(Session,ad,2,PrihDocuments[1],Articles[1]);
        PrihQuantity.ChangeProperty(Session,ad,10,PrihDocuments[3],Articles[1]);
        RashQuantity.ChangeProperty(Session,ad,14,RashDocuments[2],Articles[1]);

        PrihQuantity.ChangeProperty(Session,ad,8,PrihDocuments[3],Articles[2]);
        RashQuantity.ChangeProperty(Session,ad,2,RashDocuments[1],Articles[2]);
        RashQuantity.ChangeProperty(Session,ad,10,RashDocuments[3],Articles[2]);
        PrihQuantity.ChangeProperty(Session,ad,4,PrihDocuments[4],Articles[2]);

        PrihQuantity.ChangeProperty(Session,ad,4,PrihDocuments[3],Articles[3]);
        
        RashQuantity.ChangeProperty(Session,ad,4,RashDocuments[2],Articles[4]);
        RashQuantity.ChangeProperty(Session,ad,4,RashDocuments[3],Articles[4]);

        Quantity.ChangeProperty(Session,ad,8,PrihDocuments[1],Articles[4]);
        Quantity.ChangeProperty(Session,ad,10,PrihDocuments[1],Articles[2]);
        Quantity.ChangeProperty(Session,ad,20,PrihDocuments[1],Articles[3]);
        Quantity.ChangeProperty(Session,ad,28,PrihDocuments[1],Articles[4]);
        Quantity.ChangeProperty(Session,ad,7,PrihDocuments[2],Articles[1]);
        Quantity.ChangeProperty(Session,ad,40,PrihDocuments[2],Articles[2]);
        Quantity.ChangeProperty(Session,ad,50,PrihDocuments[2],Articles[3]);
        Quantity.ChangeProperty(Session,ad,68,PrihDocuments[2],Articles[4]);

//        List<AggregateProperty> UpdateProps = new ArrayList();
//        UpdateProps.add((AggregateProperty)QDep.Property);
//        UpdateAggregations(ad, UpdateProps, Session);

//        ((ObjectProperty)QDep.Property).OutChangesTable(ad, Session);

        Apply(ad,Session);

//        OstArtStore.Property.Out(ad);
//        OstArt.Property.Out(ad);
//        ((ObjectProperty)MaxPrih.Property).OutChangesTable(ad, Session);
//        MaxPrih.Property.Out(ad);
        PrihArtStore.Property.Out(ad);
        RashArtStore.Property.Out(ad);
        MaxOpStore.Property.Out(ad);

//        UpdateProps.add((AggregateProperty)GAP.Property);
//        UpdateAggregations(ad, UpdateProps, Session);

//        ((ObjectProperty)G2P.Property).OutChangesTable(ad, Session);

        ((AggregateProperty)GSum.Property).CheckAggregation(ad,"GSum");
        ((AggregateProperty)GP.Property).CheckAggregation(ad,"GP");
        ((AggregateProperty)G2P.Property).CheckAggregation(ad,"G2P");
        ((AggregateProperty)GAP.Property).CheckAggregation(ad,"GAP");
//        ((AggregateProperty)OstArtStore.Property).CheckAggregation(ad,"OstArtStore");
        ((AggregateProperty)OstArt.Property).CheckAggregation(ad,"OstArt");
        ((AggregateProperty)MaxPrih.Property).CheckAggregation(ad,"MaxPrih");
        ((AggregateProperty)MaxOpStore.Property).CheckAggregation(ad,"MaxOpStore");
        
//        Name.Property.Out(ad);
//        GP.Property.Out(ad);
//        QDep.Property.Out(ad);

        Session = new ChangesSession(1);
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

        PrihQuantity.ChangeProperty(Session,ad,2,PrihDocuments[3],Articles[1]);
        PrihQuantity.ChangeProperty(Session,ad,23,PrihDocuments[3],Articles[4]);
        PrihQuantity.ChangeProperty(Session,ad,1,PrihDocuments[1],Articles[0]);
        RashQuantity.ChangeProperty(Session,ad,111,RashDocuments[1],Articles[5]);
        RashQuantity.ChangeProperty(Session,ad,2222,RashDocuments[4],Articles[3]);

        DocDate.ChangeProperty(Session,ad,1002,PrihDocuments[2]);
        DocStore.ChangeProperty(Session,ad,Stores[1],PrihDocuments[5]);
        Quantity.ChangeProperty(Session,ad,1,PrihDocuments[3],Articles[3]);
        DocStore.ChangeProperty(Session,ad,Stores[0],PrihDocuments[1]);
//        Quantity.ChangeProperty(Session,ad,3,Documents[0],Articles[3]);
//        ArtToGroup.ChangeProperty(Session,ad,ArticleGroups[0],Articles[3]);

//        ((ObjectProperty)DocStore.Property).OutChangesTable(ad, Session);
        
//        List<AggregateProperty> UpdateProps = new ArrayList();
//        UpdateProps.add((AggregateProperty)G2P.Property);
//        UpdateProps.add((AggregateProperty)GP.Property);
//        UpdateProps.add((AggregateProperty)GAP.Property);
//        UpdateAggregations(ad, UpdateProps, Session);

//        ((ObjectProperty)GP.Property).OutChangesTable(ad, Session);
        
        Apply(ad,Session);

        MaxPrih.Property.Out(ad);

//        OstArt.Property.Out(ad);
        
        MaxOpStore.Property.Out(ad);
//        GSum.Property.Out(ad);
//        GP.Property.Out(ad);
//        G2P.Property.Out(ad);
        ((AggregateProperty)GSum.Property).CheckAggregation(ad,"GSum");
        ((AggregateProperty)GP.Property).CheckAggregation(ad,"GP");
        ((AggregateProperty)G2P.Property).CheckAggregation(ad,"G2P");
        ((AggregateProperty)GAP.Property).CheckAggregation(ad,"GAP");
//        ((AggregateProperty)OstArtStore.Property).CheckAggregation(ad,"OstArtStore");
        ((AggregateProperty)OstArt.Property).CheckAggregation(ad,"OstArt");
        ((AggregateProperty)MaxPrih.Property).CheckAggregation(ad,"MaxPrih");
        ((AggregateProperty)MaxOpStore.Property).CheckAggregation(ad,"MaxOpStore");

        if(true) return;
        // потестим FormBeanView
        // пока один вид потестим
        FormBeanView fbv  = new FormBeanView(ad,this);
        ObjectImplement obj1 = new ObjectImplement();                
        ObjectImplement obj2 = new ObjectImplement();
        
        GroupObjectImplement gv = new GroupObjectImplement();
        GroupObjectImplement gv2 = new GroupObjectImplement();

        gv.add(obj1);
        gv2.add(obj2);
        fbv.AddGroup(gv);
        fbv.AddGroup(gv2);
        gv.GID = 1;
        gv2.GID = 2;

        PropertyObjectImplement GrTovImpl=null;
        PropertyObjectImplement NameImpl=null;
        
        // закинем все одиночные
        Iterator<Property> ipr = Properties.iterator();
        while(ipr.hasNext()) {
            Property DrawProp = ipr.next();
            if(DrawProp.Interfaces.size() == 1) {
                PropertyObjectImplement PropImpl = new PropertyObjectImplement(DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj1);
                if(DrawProp.OutName.equals("имя"))
                    NameImpl = PropImpl;
                fbv.Properties.add(new PropertyView(PropImpl,gv));
                
                PropImpl = new PropertyObjectImplement(DrawProp);
                PropImpl.Mapping.put((PropertyInterface)DrawProp.Interfaces.iterator().next(),obj2);
                if(DrawProp.OutName.equals("гр. тов"))
                    GrTovImpl = PropImpl;
                fbv.Properties.add(new PropertyView(PropImpl,gv2));
            }
        }
        
        PropertyObjectImplement QImpl = AddPropView(fbv,Quantity,gv2,obj2,obj1);
        AddPropView(fbv,GP,gv2,obj2,obj1);

        GroupObjectValue ChangeValue;
        
        obj1.OutName = "товар";
        fbv.ChangeClass(obj1,Base.ID);
        obj2.OutName = "док";
        fbv.ChangeClass(obj2,Document.ID);
        fbv.AddFilter(new NotNullFilter(QImpl));
        fbv.AddOrder(NameImpl);
        
        fbv.EndApply().Out(fbv);

        ChangeValue = new GroupObjectValue();
        ChangeValue.put(obj1,8);
        ChangeValue.put(obj2,13);
        fbv.ChangeObject(gv,ChangeValue);
        
        fbv.EndApply().Out(fbv);

/*        ChangeValue = new GroupObjectValue();
        ChangeValue.put(obj1,3);
        ChangeValue.put(obj2,7);
        fbv.ChangeObject(gv,ChangeValue);
        
        fbv.EndApply().Out(fbv);
*/
        fbv.ChangeClass(obj1,Article.ID);
        fbv.ChangeClassView(gv, false);
        fbv.EndApply().Out(fbv);

        fbv.ChangeClassView(gv, true);
        fbv.EndApply().Out(fbv);

//        DataPropertyInterface[] ToDraw = new DataPropertyInterface[1];
//        ToDraw[0] = new DataPropertyInterface(Article);
//        ToDraw[1] = new DataPropertyInterface(Store);
//        DisplayClasses(ad,ToDraw);

        ad.Disconnect();
    }
    
    PropertyObjectImplement AddPropView(FormBeanView fbv,LP ListProp,GroupObjectImplement gv,ObjectImplement... Params) {
        PropertyObjectImplement PropImpl = new PropertyObjectImplement(ListProp.Property);
        
        ListIterator<PropertyInterface> i = ListProp.ListInterfaces.listIterator();
        for(ObjectImplement Object : Params) {
            PropImpl.Mapping.put(i.next(),Object);
        }
        fbv.Properties.add(new PropertyView(PropImpl,gv));
        return PropImpl;
    }
    
    // "рисует" класс, со всеми св-вами
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
            // попробуем все варианты отображения
            Iterator<Map<PropertyInterface,DataPropertyInterface>> im = Maps.iterator();
            while(im.hasNext()) {
                Map<PropertyInterface,DataPropertyInterface> Impl = im.next();
                Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();
                
                Iterator<PropertyInterface> ip = Prop.Interfaces.iterator();
                while(ip.hasNext()) {
                    PropertyInterface Interface = ip.next();
                    DataPropertyInterface MapInterface = Impl.get(Interface);
                    Interface.ValueClass = MapInterface.Class;
                    JoinImplement.put(Interface,JoinSources.get(MapInterface));
                }
                
                if(Prop.GetValueClass()!=null) {
                    // то есть актуальное св-во
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

// для упрощенного создания св-в со списками интерфейсов, по сути как фасад

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
    
    void ChangeProperty(DataAdapter Adapter,Object Value,Integer ...iParams) throws SQLException {
        Map<DataPropertyInterface,Integer> Keys = new HashMap<DataPropertyInterface,Integer>();
        Integer IntNum = 0;
        for(int i : iParams) {
            Keys.put((DataPropertyInterface)ListInterfaces.get(IntNum),i);
            IntNum++;
        }
        
        ((DataProperty)Property).ChangeProperty(Adapter, Keys, Value);
    }
    
    void ChangeProperty(ChangesSession Session,DataAdapter Adapter,Object Value,Integer ...iParams) throws SQLException {
        Map<DataPropertyInterface,Integer> Keys = new HashMap<DataPropertyInterface,Integer>();
        Integer IntNum = 0;
        for(int i : iParams) {
            Keys.put((DataPropertyInterface)ListInterfaces.get(IntNum),i);
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