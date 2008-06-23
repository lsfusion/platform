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

        Props[7] = AddGProp(Props[4],Props[8],1);
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
        SelectQuery SimpleSelect = new SelectQuery(new SelectTable("dumb"));
        // Props[0] проверим
        // для этого сначала в JoinImplement закинем
        SourceExpr SourceJoin = new ValueSourceExpr(1);
        SourceExpr SourceJoin2 = new ValueSourceExpr(2);

        JoinList Joins=new JoinList();
        
        Props[7].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.add(new SelectExpression(Props[7].Property.JoinSelect(Joins),"test1"));
        Props[0].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.add(new SelectExpression(Props[0].Property.JoinSelect(Joins),"test1"));
        Props[9].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.add(new SelectExpression(Props[9].Property.JoinSelect(Joins),"test2"));
        Props[2].ListInterfaces.get(0).JoinImplement = SourceJoin;
        Props[2].ListInterfaces.get(1).JoinImplement = SourceJoin2;
        SimpleSelect.Expressions.add(new SelectExpression(Props[2].Property.JoinSelect(Joins),"test3"));
        Props[4].ListInterfaces.get(0).JoinImplement = SourceJoin;
        Props[4].ListInterfaces.get(1).JoinImplement = SourceJoin2;
        SimpleSelect.Expressions.add(new SelectExpression(Props[4].Property.JoinSelect(Joins),"test4"));
        Props[6].ListInterfaces.get(0).JoinImplement = SourceJoin;
        SimpleSelect.Expressions.add(new SelectExpression(Props[6].Property.JoinSelect(Joins),"test5"));
        Props[10].ListInterfaces.get(0).JoinImplement = SourceJoin;
        Props[10].ListInterfaces.get(1).JoinImplement = SourceJoin2;
        SimpleSelect.Expressions.add(new SelectExpression(Props[10].Property.JoinSelect(Joins),"test6"));

        Iterator<Select> ij = Joins.iterator();
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
    
    LGP AddGProp(LP GroupProp,Object ...Params) {
        GroupProperty Property = new GroupProperty(TableFactory,(SourceProperty)GroupProp.Property);
        LGP ListProperty = new LGP(Property,GroupProp);
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(GroupProp,Params);
        ListIterator<PropertyInterfaceImplement> i = PropImpl.listIterator();
        while(i.hasNext()) ListProperty.AddInterface(i.next());
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
        Class String = new StringClass(0);
        Class Integer = new QuantityClass(1);
        
        Class Base = new ObjectClass(2);
        Class Article = new ObjectClass(3);
        Article.AddParent(Base);
        Class Store = new ObjectClass(4);
        Store.AddParent(Base);
        Class Document = new ObjectClass(5);
        Document.AddParent(Base);
        Class ArticleGroup = new ObjectClass(6);
        ArticleGroup.AddParent(Base);
        
        LDP Name = AddDProp(String,Base);
        LDP DocStore = AddDProp(Store,Document);
        LDP Quantity = AddDProp(Integer,Document,Article);
        LDP ArtToGroup = AddDProp(ArticleGroup,Article);

        AddRProp(Name,1,DocStore,1);
        AddRProp(Name,1,ArtToGroup,1);
        
        LGP GP = AddGProp(Quantity,DocStore,1,2);
        AddGProp(GP,2);
        
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

        DataAdapter ad = new DataAdapter();
        ad.Connect("");

        FillDB(ad);
        
        Integer i;
        Integer[] Articles = new Integer[5];
        for(i=0;i<Articles.length;i++) Articles[i]=Article.AddObject(ad, TableFactory);
        Name.ChangeProperty(ad,"КОЛБАСА", Articles[0]);
        Name.ChangeProperty(ad,"ТВОРОГ", Articles[1]);
        Name.ChangeProperty(ad,"МОЛОКО", Articles[2]);
        Name.ChangeProperty(ad,"ОБУВЬ", Articles[3]);
        Name.ChangeProperty(ad,"ДЖЕМПЕР", Articles[4]);

        Integer[] Stores = new Integer[2];
        for(i=0;i<Stores.length;i++) Stores[i]=Store.AddObject(ad, TableFactory);
        Name.ChangeProperty(ad,"СКЛАД", Stores[0]);
        Name.ChangeProperty(ad,"ТЗАЛ", Stores[1]);
        
        Integer[] Documents = new Integer[6];
        for(i=0;i<Documents.length;i++) {
            Documents[i]=Document.AddObject(ad, TableFactory);
            Name.ChangeProperty(ad,"ДОК "+i.toString(), Documents[i]);
        }
        DocStore.ChangeProperty(ad,Stores[0],Documents[0]);
        DocStore.ChangeProperty(ad,Stores[0],Documents[1]);
        DocStore.ChangeProperty(ad,Stores[1],Documents[2]);
        DocStore.ChangeProperty(ad,Stores[0],Documents[3]);
        DocStore.ChangeProperty(ad,Stores[1],Documents[4]);
        DocStore.ChangeProperty(ad,Stores[1],Documents[5]);
        
        Integer[] ArticleGroups = new Integer[2];
        for(i=0;i<ArticleGroups.length;i++) ArticleGroups[i]=ArticleGroup.AddObject(ad, TableFactory);
        Name.ChangeProperty(ad,"ПРОДУКТЫ", ArticleGroups[0]);
        Name.ChangeProperty(ad,"ОДЕЖДА", ArticleGroups[1]);
        
        ArtToGroup.ChangeProperty(ad,ArticleGroups[0],Articles[0]);
        ArtToGroup.ChangeProperty(ad,ArticleGroups[0],Articles[1]);
        ArtToGroup.ChangeProperty(ad,ArticleGroups[0],Articles[2]);
        ArtToGroup.ChangeProperty(ad,ArticleGroups[1],Articles[3]);
        ArtToGroup.ChangeProperty(ad,ArticleGroups[1],Articles[4]);

        Quantity.ChangeProperty(ad,2,Documents[0],Articles[0]);
        Quantity.ChangeProperty(ad,5,Documents[0],Articles[1]);
        Quantity.ChangeProperty(ad,7,Documents[1],Articles[1]);
        Quantity.ChangeProperty(ad,2,Documents[1],Articles[3]);
        Quantity.ChangeProperty(ad,8,Documents[1],Articles[4]);
        Quantity.ChangeProperty(ad,10,Documents[1],Articles[2]);
        Quantity.ChangeProperty(ad,20,Documents[1],Articles[3]);
        Quantity.ChangeProperty(ad,28,Documents[1],Articles[4]);

        DataPropertyInterface[] ToDraw = new DataPropertyInterface[1];
        ToDraw[0] = new DataPropertyInterface(Article);
//        ToDraw[1] = new DataPropertyInterface(Store);
        DisplayClasses(ad,ToDraw);

        ad.Disconnect();
    }
    
    // "рисует" класс, со всеми св-вами
    void DisplayClasses(DataAdapter Adapter, DataPropertyInterface[] ToDraw) throws SQLException {

        Map<DataPropertyInterface,SourceExpr> JoinSources = new HashMap<DataPropertyInterface,SourceExpr>();
        SelectQuery SimpleSelect = new SelectQuery(null);
        SelectTable PrevSelect = null;
        for(int ic=0;ic<ToDraw.length;ic++) {
            SelectTable Select = TableFactory.ObjectTable.ClassSelect(ToDraw[ic].Class);
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
                
                Iterator<PropertyInterface> ip = Prop.Interfaces.iterator();
                while(ip.hasNext()) {
                    PropertyInterface Interface = ip.next();
                    DataPropertyInterface MapInterface = Impl.get(Interface);
                    Interface.ValueClass = MapInterface.Class;
                    Interface.JoinImplement = JoinSources.get(MapInterface);
                }
                
                if(Prop.GetValueClass()!=null) {
                    // то есть актуальное св-во
                    SimpleSelect.Expressions.add(new SelectExpression(Prop.JoinSelect(Joins),"test"+(SelFields++).toString()));
                }
            }
        }
        
        Iterator<Select> ij = Joins.iterator();
        while(ij.hasNext()) {
            Select Join = ij.next();
            Join.JoinType = "LEFT";
            PrevSelect.Joins.add(Join);
        }

        Adapter.OutSelect(SimpleSelect);
    }
}

class MapBuilder<T,V> {
    
    void RecBuildMap(T[] From,V[] To,int iFr,List<Map<T,V>> Result,HashMap<T,V> CurrentMap) {
        if(iFr==From.length) {
            Result.add((Map<T,V>)CurrentMap.clone());
            return;
        }

        for(int v=0;v<To.length;v++)
            if(!CurrentMap.containsValue(To[v])){
                CurrentMap.put(From[iFr],To[v]);
                RecBuildMap(From,To,iFr+1,Result,CurrentMap);
                CurrentMap.remove(From[iFr]);
            }
    }
    
    List<Map<T,V>> BuildMap(T[] From,V[] To) {
        List<Map<T,V>> Result = new ArrayList<Map<T,V>>();
        RecBuildMap(From,To,0,Result,new HashMap<T,V>(0));
        return Result;
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